package fu.se.pharmacy;

import fu.se.pharmacy.dto.RefundRequestDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.SystemSettingService;
import fu.se.pharmacy.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FIX: RefundServiceImpl khong con tu sua InventoryBatch — goi
 * InventoryService.restoreForCancelledSale() (Nguoi 2 so huu). Va khong con hardcode
 * han muc 2 trieu, ma doc tu SystemSettingService (mac dinh 500_000 neu Admin chua cau hinh,
 * dung bang gia tri seed REFUND_APPROVAL_LIMIT trong SQL).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService - Unit Tests")
class RefundServiceTest {

    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private InventoryService inventoryService;
    @Mock private SystemSettingService systemSettingService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private RefundServiceImpl refundService;

    private Sale completedSale;
    private RefundRequest pendingRequest;

    @BeforeEach
    void setUp() {
        completedSale = new Sale();
        completedSale.setSaleId(1);
        completedSale.setStatus("COMPLETED");
        completedSale.setFinalAmount(105000);
        completedSale.setSaleCode("SALE-001");

        pendingRequest = new RefundRequest();
        pendingRequest.setRefundRequestId(1);
        pendingRequest.setSaleId(1);
        pendingRequest.setRequestedBy(5);
        pendingRequest.setRefundAmount(105000);
        pendingRequest.setStatus("PENDING");
        pendingRequest.setReason("Khach mua nham");
        pendingRequest.setRequestedAt(LocalDateTime.now());
    }

    // ===========================================================
    // createRequest
    // ===========================================================

    @Nested
    @DisplayName("createRequest")
    class CreateRequest {

        @Test
        @DisplayName("Tao yeu cau hoan tien - thanh cong")
        void createRequest_success() {
            RefundRequestDTO input = new RefundRequestDTO();
            input.setSaleId(1);
            input.setReason("Khach phan hoi mua nham, muon doi don");

            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(refundRequestRepository.findBySaleId(1)).thenReturn(List.of());
            when(refundRequestRepository.save(any())).thenReturn(pendingRequest);
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());
            // FIX: impl dùng findLatestBySaleId() → gọi findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findLatestBySaleId(1)).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.createRequest(input, 5);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getRefundAmount()).isEqualTo(105000);
            verify(refundRequestRepository).save(any(RefundRequest.class));
        }

        @Test
        @DisplayName("Tao yeu cau - hoa don khong o COMPLETED - nem exception")
        void createRequest_nonCompletedSale_throwsException() {
            completedSale.setStatus("DRAFT");
            RefundRequestDTO input = new RefundRequestDTO();
            input.setSaleId(1);
            input.setReason("Li do test");

            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));

            assertThatThrownBy(() -> refundService.createRequest(input, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("COMPLETED");
        }

        @Test
        @DisplayName("Tao yeu cau - da co PENDING truoc do - nem exception")
        void createRequest_existingPending_throwsException() {
            RefundRequestDTO input = new RefundRequestDTO();
            input.setSaleId(1);
            input.setReason("Li do test");

            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(refundRequestRepository.findBySaleId(1)).thenReturn(List.of(pendingRequest));

            assertThatThrownBy(() -> refundService.createRequest(input, 5))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    // ===========================================================
    // approveByManager
    // ===========================================================

    @Nested
    @DisplayName("approveByManager")
    class ApproveByManager {

        @Test
        @DisplayName("Manager duyet - so tien <= han muc (500k), phuong thuc CASH - thanh cong")
        void approveByManager_cash_withinLimit_success() {
            pendingRequest.setRefundAmount(300_000);

            Payment cashPayment = new Payment();
            cashPayment.setPaymentMethod("CASH");
            cashPayment.setStatus("PAID");
            cashPayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(systemSettingService.getMoneyLimit(anyString(), any()))
                    .thenReturn(BigDecimal.valueOf(500_000));
            // FIX: impl gọi findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findLatestBySaleId(1))
                    .thenReturn(Optional.of(cashPayment));
            when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());
            when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefundRequestDTO result = refundService.approveByManager(1, 2);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            verify(inventoryService).restoreForCancelledSale(1);
        }

        @Test
        @DisplayName("Manager duyet - so tien > han muc (500k) - nem exception")
        void approveByManager_aboveLimit_throwsException() {
            pendingRequest.setRefundAmount(3_000_000);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(systemSettingService.getMoneyLimit(anyString(), any()))
                    .thenReturn(BigDecimal.valueOf(500_000));
            // FIX: impl check hạn mức trước khi check payment method
            // nên không cần mock paymentRepository ở đây

            assertThatThrownBy(() -> refundService.approveByManager(1, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Admin");

            verify(inventoryService, never()).restoreForCancelledSale(any());
        }

        @Test
        @DisplayName("Manager duyet - phuong thuc ONLINE - nem exception")
        void approveByManager_onlinePayment_throwsException() {
            pendingRequest.setRefundAmount(300_000);

            Payment onlinePayment = new Payment();
            onlinePayment.setPaymentMethod("ONLINE");
            onlinePayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(systemSettingService.getMoneyLimit(anyString(), any()))
                    .thenReturn(BigDecimal.valueOf(500_000));
            // FIX: findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findLatestBySaleId(1))
                    .thenReturn(Optional.of(onlinePayment));

            assertThatThrownBy(() -> refundService.approveByManager(1, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Admin");
        }

        @Test
        @DisplayName("Manager duyet - yeu cau khong o PENDING - nem exception")
        void approveByManager_nonPending_throwsException() {
            pendingRequest.setStatus("APPROVED");

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> refundService.approveByManager(1, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    // ===========================================================
    // approveByAdmin
    // ===========================================================

    @Nested
    @DisplayName("approveByAdmin")
    class ApproveByAdmin {

        @Test
        @DisplayName("Admin duyet - bat ky so tien - thanh cong")
        void approveByAdmin_anyAmount_success() {
            pendingRequest.setRefundAmount(5_000_000);

            Payment onlinePayment = new Payment();
            onlinePayment.setPaymentMethod("ONLINE");
            onlinePayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            // FIX: findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findLatestBySaleId(1))
                    .thenReturn(Optional.of(onlinePayment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.approveByAdmin(1, 1);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            verify(inventoryService).restoreForCancelledSale(1);
        }

        @Test
        @DisplayName("Admin duyet - yeu cau khong o PENDING - nem exception")
        void approveByAdmin_nonPending_throwsException() {
            pendingRequest.setStatus("REJECTED");
            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> refundService.approveByAdmin(1, 1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    // ===========================================================
    // reject
    // ===========================================================

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("Tu choi yeu cau - thanh cong, status = REJECTED")
        void reject_success() {
            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(refundRequestRepository.save(any())).thenAnswer(inv -> {
                RefundRequest r = inv.getArgument(0);
                assertThat(r.getStatus()).isEqualTo("REJECTED");
                assertThat(r.getApprovedBy()).isEqualTo(2);
                return r;
            });
            // FIX: toDTO() gọi findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findLatestBySaleId(1)).thenReturn(Optional.empty());
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.reject(1, 2);
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }
    }

    // ===========================================================
    // doApprove - uy quyen hoan kho cho InventoryService
    // ===========================================================
    // FIX: Logic FIFO hoan lo / khoi phuc lo DISPOSED da chuyen sang
    // InventoryServiceImpl.restoreForCancelledSale() (Nguoi 2 so huu, co test rieng trong
    // InventoryServiceTest). RefundServiceImpl gio chi can uy quyen dung ham nay sau khi duyet.

    @Test
    @DisplayName("Sau khi duyet - Sale chuyen REFUNDED va goi InventoryService.restoreForCancelledSale")
    void approve_delegatesRestoreToInventoryService() {
        Payment cashPayment = new Payment();
        cashPayment.setPaymentMethod("CASH");
        cashPayment.setSaleId(1);

        pendingRequest.setRefundAmount(300_000);

        when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
        when(systemSettingService.getMoneyLimit(anyString(), any()))
                .thenReturn(BigDecimal.valueOf(500_000));
        // FIX: findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
        when(paymentRepository.findLatestBySaleId(1))
                .thenReturn(Optional.of(cashPayment));
        when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            assertThat(s.getStatus()).isEqualTo("REFUNDED");
            return s;
        });
        when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

        refundService.approveByManager(1, 2);

        verify(inventoryService).restoreForCancelledSale(1);
        verify(saleRepository).save(argThat(s -> "REFUNDED".equals(s.getStatus())));
    }
}