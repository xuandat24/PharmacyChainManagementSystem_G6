package fu.se.pharmacy;

import fu.se.pharmacy.dto.RefundRequestDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.impl.RefundServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundService - Unit Tests")
class RefundServiceTest {

    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private SaleDetailRepository saleDetailRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private AppUserRepository appUserRepository;

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
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());
            when(paymentRepository.findBySaleId(1)).thenReturn(Optional.empty());

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
        @DisplayName("Manager duyet - so tien <= 2tr, phuong thuc CASH - thanh cong")
        void approveByManager_cash_withinLimit_success() {
            pendingRequest.setRefundAmount(1_500_000); // <= 2tr

            Payment cashPayment = new Payment();
            cashPayment.setPaymentMethod("CASH");
            cashPayment.setStatus("PAID");
            cashPayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(paymentRepository.findBySaleId(1)).thenReturn(Optional.of(cashPayment));
            when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());
            when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefundRequestDTO result = refundService.approveByManager(1, 2);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("Manager duyet - so tien > 2tr - nem exception")
        void approveByManager_aboveLimit_throwsException() {
            pendingRequest.setRefundAmount(3_000_000); // > 2tr

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));

            assertThatThrownBy(() -> refundService.approveByManager(1, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Admin");
        }

        @Test
        @DisplayName("Manager duyet - phuong thuc ONLINE - nem exception")
        void approveByManager_onlinePayment_throwsException() {
            pendingRequest.setRefundAmount(500_000); // trong han muc

            Payment onlinePayment = new Payment();
            onlinePayment.setPaymentMethod("ONLINE");
            onlinePayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(paymentRepository.findBySaleId(1)).thenReturn(Optional.of(onlinePayment));

            assertThatThrownBy(() -> refundService.approveByManager(1, 2))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Admin");
        }

        @Test
        @DisplayName("Manager duyet - yeu cau khong o PENDING - nem exception")
        void approveByManager_nonPending_throwsException() {
            pendingRequest.setStatus("APPROVED"); // da duyet roi

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
            pendingRequest.setRefundAmount(5_000_000); // vuot han muc manager

            Payment onlinePayment = new Payment();
            onlinePayment.setPaymentMethod("ONLINE");
            onlinePayment.setSaleId(1);

            when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
            when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
            when(paymentRepository.findBySaleId(1)).thenReturn(Optional.of(onlinePayment));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.approveByAdmin(1, 1);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
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
            when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
            when(paymentRepository.findBySaleId(1)).thenReturn(Optional.empty());
            when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

            RefundRequestDTO result = refundService.reject(1, 2);
            assertThat(result.getStatus()).isEqualTo("REJECTED");
        }
    }

    // ===========================================================
    // doApprove - restore inventory
    // ===========================================================

    @Test
    @DisplayName("Sau khi duyet - ton kho duoc hoan lai chinh xac")
    void approve_restoresInventory_correctly() {
        SaleDetail detail = new SaleDetail();
        detail.setSaleDetailId(1);
        detail.setSaleId(1);
        detail.setMedicineId(1);
        detail.setQuantity(10);
        detail.setInventoryBatchId(5); // lo da tru

        InventoryBatch batch = new InventoryBatch();
        batch.setInventoryBatchId(5);
        batch.setQuantityOnHand(90); // ban 10 con 90
        batch.setStatus("AVAILABLE");

        Payment cashPayment = new Payment();
        cashPayment.setPaymentMethod("CASH");
        cashPayment.setSaleId(1);

        pendingRequest.setRefundAmount(1_000_000); // trong han muc manager

        when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
        when(paymentRepository.findBySaleId(1)).thenReturn(Optional.of(cashPayment));
        when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(detail));
        when(inventoryBatchRepository.findById(5)).thenReturn(Optional.of(batch));
        when(inventoryBatchRepository.save(any())).thenAnswer(inv -> {
            InventoryBatch b = inv.getArgument(0);
            assertThat(b.getQuantityOnHand()).isEqualTo(100); // 90 + 10 hoan lai
            return b;
        });
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

        refundService.approveByManager(1, 2);

        verify(inventoryBatchRepository).save(argThat(b -> b.getQuantityOnHand() == 100));
    }

    @Test
    @DisplayName("Sau khi duyet - lo da DISPOSED duoc khoi phuc AVAILABLE")
    void approve_restoresDisposedBatch_toAvailable() {
        SaleDetail detail = new SaleDetail();
        detail.setSaleId(1);
        detail.setMedicineId(1);
        detail.setQuantity(5);
        detail.setInventoryBatchId(7);

        InventoryBatch disposedBatch = new InventoryBatch();
        disposedBatch.setInventoryBatchId(7);
        disposedBatch.setQuantityOnHand(0);
        disposedBatch.setStatus("DISPOSED"); // lo da tieu thu het

        Payment cashPayment = new Payment();
        cashPayment.setPaymentMethod("CASH");
        cashPayment.setSaleId(1);

        pendingRequest.setRefundAmount(500_000);

        when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pendingRequest));
        when(paymentRepository.findBySaleId(1)).thenReturn(Optional.of(cashPayment));
        when(refundRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.findById(1)).thenReturn(Optional.of(completedSale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(detail));
        when(inventoryBatchRepository.findById(7)).thenReturn(Optional.of(disposedBatch));
        when(inventoryBatchRepository.save(any())).thenAnswer(inv -> {
            InventoryBatch b = inv.getArgument(0);
            assertThat(b.getStatus()).isEqualTo("AVAILABLE"); // khoi phuc
            assertThat(b.getQuantityOnHand()).isEqualTo(5);
            return b;
        });
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appUserRepository.findById(anyInt())).thenReturn(Optional.empty());

        refundService.approveByManager(1, 2);

        verify(inventoryBatchRepository).save(argThat(b ->
                "AVAILABLE".equals(b.getStatus()) && b.getQuantityOnHand() == 5));
    }
}
