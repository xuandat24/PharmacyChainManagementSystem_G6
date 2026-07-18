package fu.se.pharmacy;

import fu.se.pharmacy.dto.PaymentDTO;
import fu.se.pharmacy.entity.Payment;
import fu.se.pharmacy.entity.Sale;
import fu.se.pharmacy.repository.PaymentRepository;
import fu.se.pharmacy.repository.PaymentTransactionRepository;
import fu.se.pharmacy.repository.SaleRepository;
import fu.se.pharmacy.service.SaleService;
import fu.se.pharmacy.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService - Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private SaleService saleService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private Sale draftSale;

    @BeforeEach
    void setUp() {
        draftSale = new Sale();
        draftSale.setSaleId(1);
        draftSale.setBranchId(1);
        draftSale.setPharmacistId(5);
        draftSale.setStatus("DRAFT");
        draftSale.setFinalAmount(24000);
        draftSale.setTotalAmount(24000);
        draftSale.setDiscountAmount(0);
    }

    // ===========================================================
    // processCash
    // ===========================================================

    @Nested
    @DisplayName("processCash")
    class ProcessCash {

        @Test
        @DisplayName("Thanh toan tien mat thanh cong - tra du tien")
        void processCash_success() {
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(50000);

            Payment savedPayment = new Payment();
            savedPayment.setPaymentId(1);
            savedPayment.setSaleId(1);
            savedPayment.setPaymentMethod("CASH");
            savedPayment.setAmount(24000);
            savedPayment.setCustomerPaidAmount(50000);
            savedPayment.setChangeAmount(26000);
            savedPayment.setStatus("PAID");

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            // FIX: impl gọi findBySaleIdOrderByCreatedAtDesc để check duplicate
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1)).thenReturn(List.of());
            when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
            doNothing().when(saleService).completeSale(1);

            PaymentDTO result = paymentService.processCash(input);

            assertThat(result.getStatus()).isEqualTo("PAID");
            assertThat(result.getPaymentMethod()).isEqualTo("CASH");
            assertThat(result.getAmount()).isEqualTo(24000);
            assertThat(result.getChangeAmount()).isEqualTo(26000);
            verify(saleService).completeSale(1);
        }

        @Test
        @DisplayName("Thanh toan tien mat - tien khong du - nem exception")
        void processCash_insufficientAmount_throwsException() {
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(10000); // can 24000

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            // FIX: impl check duplicate trước khi check số tiền
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1)).thenReturn(List.of());

            // FIX: impl ném "Tiền khách đưa không đủ" (tiếng Việt có dấu)
            // → dùng isInstanceOf thay vì hasMessageContaining tiếng không dấu
            assertThatThrownBy(() -> paymentService.processCash(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("không đủ");
            verify(saleService, never()).completeSale(any());
        }

        @Test
        @DisplayName("Thanh toan tien mat - tien = 0 - nem exception")
        void processCash_zeroAmount_throwsException() {
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(0);

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1)).thenReturn(List.of());

            assertThatThrownBy(() -> paymentService.processCash(input))
                    .isInstanceOf(RuntimeException.class);
            verify(saleService, never()).completeSale(any());
        }

        @Test
        @DisplayName("Thanh toan tien mat - hoa don khong o DRAFT - nem exception")
        void processCash_nonDraftSale_throwsException() {
            draftSale.setStatus("COMPLETED");
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(50000);

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            // FIX: impl kiểm tra status TRƯỚC khi check duplicate
            // nên không cần mock findBySaleIdOrderByCreatedAtDesc ở đây

            assertThatThrownBy(() -> paymentService.processCash(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DRAFT");
            verify(saleService, never()).completeSale(any());
        }

        @Test
        @DisplayName("Thanh toan tien mat - tien vua du chinh xac - thanh cong, tien thua = 0")
        void processCash_exactAmount_noChange() {
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(24000);

            Payment savedPayment = new Payment();
            savedPayment.setPaymentId(2);
            savedPayment.setAmount(24000);
            savedPayment.setCustomerPaidAmount(24000);
            savedPayment.setChangeAmount(0);
            savedPayment.setStatus("PAID");
            savedPayment.setPaymentMethod("CASH");

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1)).thenReturn(List.of());
            when(paymentRepository.save(any())).thenReturn(savedPayment);
            doNothing().when(saleService).completeSale(1);

            PaymentDTO result = paymentService.processCash(input);
            assertThat(result.getChangeAmount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Thanh toan tien mat - hoa don da PAID - nem exception chong duplicate")
        void processCash_alreadyPaid_throwsException() {
            PaymentDTO input = new PaymentDTO();
            input.setSaleId(1);
            input.setCustomerPaidAmount(50000);

            // Đã có payment PAID → chặn không cho thanh toán lại
            Payment existingPaid = new Payment();
            existingPaid.setPaymentId(1);
            existingPaid.setSaleId(1);
            existingPaid.setStatus("PAID");

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1))
                    .thenReturn(List.of(existingPaid));

            assertThatThrownBy(() -> paymentService.processCash(input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("đã được thanh toán");
            verify(saleService, never()).completeSale(any());
        }
    }

    // ===========================================================
    // createOnlinePayment
    // ===========================================================

    @Nested
    @DisplayName("createOnlinePayment")
    class CreateOnlinePayment {

        @Test
        @DisplayName("Tao thanh toan online - tra ve payment PENDING voi QR note")
        void createOnlinePayment_success() {
            Payment pending = new Payment();
            pending.setPaymentId(1);
            pending.setSaleId(1);
            pending.setPaymentMethod("ONLINE");
            pending.setStatus("PENDING");
            pending.setAmount(24000);
            pending.setNote("PHARMACY_PAY_1_24000");

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            // FIX: impl gọi findLatestBySaleId() - dùng default method trên PaymentRepository
            // findLatestBySaleId() gọi findBySaleIdOrderByCreatedAtDesc() → trả List.of() → Optional.empty()
            when(paymentRepository.findLatestBySaleId(1)).thenReturn(Optional.empty());
            when(paymentRepository.save(any())).thenReturn(pending);

            PaymentDTO result = paymentService.createOnlinePayment(1);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getPaymentMethod()).isEqualTo("ONLINE");
            assertThat(result.getQrNote()).contains("PHARMACY_PAY");
        }

        @Test
        @DisplayName("Tao online - da co PENDING truoc do - tra ve cai cu, khong tao moi")
        void createOnlinePayment_existingPending_returnsExisting() {
            Payment existing = new Payment();
            existing.setPaymentId(1);
            existing.setSaleId(1);
            existing.setStatus("PENDING");
            existing.setPaymentMethod("ONLINE");
            existing.setAmount(24000);

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            // FIX: findLatestBySaleId() dùng findBySaleIdOrderByCreatedAtDesc → trả list có existing
            when(paymentRepository.findLatestBySaleId(1))
                    .thenReturn(Optional.of(existing));

            PaymentDTO result = paymentService.createOnlinePayment(1);

            assertThat(result.getPaymentId()).isEqualTo(1);
            verify(paymentRepository, never()).save(any());
        }
    }

    // ===========================================================
    // handleCallback
    // ===========================================================

    @Nested
    @DisplayName("handleCallback")
    class HandleCallback {

        @Test
        @DisplayName("Callback hop le - xac nhan PAID va completeSale")
        void handleCallback_valid_confirmsPayment() {
            Payment pending = new Payment();
            pending.setPaymentId(1);
            pending.setSaleId(1);
            pending.setStatus("PENDING");
            pending.setAmount(24000);

            when(paymentRepository.findById(1)).thenReturn(Optional.of(pending));
            when(paymentTransactionRepository.existsByGatewayTransactionCode("GW-001"))
                    .thenReturn(false);
            when(paymentTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(saleService).completeSale(1);

            paymentService.handleCallback(1, "GW-001", 24000, "{\"status\":\"SUCCESS\"}");

            verify(saleService).completeSale(1);
            verify(paymentRepository).save(argThat(p -> "PAID".equals(p.getStatus())));
        }

        @Test
        @DisplayName("Callback - so tien khong khop - nem exception")
        void handleCallback_amountMismatch_throwsException() {
            Payment pending = new Payment();
            pending.setPaymentId(1);
            pending.setStatus("PENDING");
            pending.setAmount(24000);

            when(paymentRepository.findById(1)).thenReturn(Optional.of(pending));
            
            // FIX: impl ném "Số tiền callback không khớp" (tiếng Việt có dấu)
            assertThatThrownBy(() -> paymentService.handleCallback(1, "GW-001", 10000, "{}"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("không khớp");
            verify(saleService, never()).completeSale(any());
        }

        @Test
        @DisplayName("Callback - ma giao dich da duoc xu ly (replay) - nem exception")
        void handleCallback_replayAttack_throwsException() {
            Payment pending = new Payment();
            pending.setPaymentId(1);
            pending.setStatus("PENDING");
            pending.setAmount(24000);

            when(paymentRepository.findById(1)).thenReturn(Optional.of(pending));
            when(paymentTransactionRepository.existsByGatewayTransactionCode("GW-DUP"))
                    .thenReturn(true);

            // FIX: impl ném "Giao dịch GW-DUP đã được xử lý trước đó" (tiếng Việt có dấu)
            assertThatThrownBy(() -> paymentService.handleCallback(1, "GW-DUP", 24000, "{}"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("đã được xử lý");
            verify(saleService, never()).completeSale(any());
        }

        @Test
        @DisplayName("Callback - payment khong o PENDING - nem exception")
        void handleCallback_nonPendingPayment_throwsException() {
            Payment paid = new Payment();
            paid.setPaymentId(1);
            paid.setStatus("PAID");
            paid.setAmount(24000);

            when(paymentRepository.findById(1)).thenReturn(Optional.of(paid));

            assertThatThrownBy(() -> paymentService.handleCallback(1, "GW-002", 24000, "{}"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("PENDING");
            verify(saleService, never()).completeSale(any());
        }
    }
}