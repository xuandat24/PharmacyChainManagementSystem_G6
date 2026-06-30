package fu.se.pharmacy;

import fu.se.pharmacy.dto.CashShiftDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.impl.CashShiftServiceImpl;
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
@DisplayName("CashShiftService - Unit Tests")
class CashShiftServiceTest {

    @Mock private CashShiftRepository cashShiftRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks private CashShiftServiceImpl cashShiftService;

    private CashShift openShift;

    @BeforeEach
    void setUp() {
        openShift = new CashShift();
        openShift.setCashShiftId(1);
        openShift.setBranchId(1);
        openShift.setPharmacistId(5);
        openShift.setStatus("OPEN");
        openShift.setOpeningCashAmount(500_000);
        openShift.setSystemCashAmount(0);
        openShift.setOpenedAt(LocalDateTime.now().minusHours(8));
    }

    // ===========================================================
    // openShift
    // ===========================================================

    @Nested
    @DisplayName("openShift")
    class OpenShift {

        @Test
        @DisplayName("Mo ca moi - chua co ca OPEN")
        void openShift_noExisting_createsNew() {
            CashShift newShift = new CashShift();
            newShift.setCashShiftId(10);
            newShift.setPharmacistId(5);
            newShift.setBranchId(1);
            newShift.setStatus("OPEN");
            newShift.setOpeningCashAmount(0);

            when(cashShiftRepository.findByPharmacistIdAndStatus(5, "OPEN"))
                    .thenReturn(Optional.empty());
            when(cashShiftRepository.save(any())).thenReturn(newShift);
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            CashShiftDTO result = cashShiftService.openShift(5, 1);

            assertThat(result.getStatus()).isEqualTo("OPEN");
            assertThat(result.getCashShiftId()).isEqualTo(10);
            verify(cashShiftRepository).save(any(CashShift.class));
        }

        @Test
        @DisplayName("Mo ca - da co ca OPEN - tra ve ca cu khong tao moi")
        void openShift_existingOpen_returnsExisting() {
            when(cashShiftRepository.findByPharmacistIdAndStatus(5, "OPEN"))
                    .thenReturn(Optional.of(openShift));
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            CashShiftDTO result = cashShiftService.openShift(5, 1);

            assertThat(result.getCashShiftId()).isEqualTo(1);
            verify(cashShiftRepository, never()).save(any());
        }
    }

    // ===========================================================
    // closeShift
    // ===========================================================

    @Nested
    @DisplayName("closeShift")
    class CloseShift {

        @Test
        @DisplayName("Chot ca - chenh lech trong nguong - status CLOSED")
        void closeShift_withinThreshold_statusClosed() {
            Sale sale = new Sale();
            sale.setSaleId(1);
            sale.setStatus("COMPLETED");
            sale.setFinalAmount(24_000);
            sale.setSaleDate(LocalDateTime.now());

            Payment cashPayment = new Payment();
            cashPayment.setPaymentMethod("CASH");
            cashPayment.setStatus("PAID");

            CashShiftDTO input = new CashShiftDTO();
            input.setActualCashAmount(524_000); // 500000 + 24000

            when(cashShiftRepository.findById(1)).thenReturn(Optional.of(openShift));
            when(saleRepository.findByBranchIdOrderBySaleDateDesc(1)).thenReturn(List.of(sale));
            // FIX: impl dùng findLatestBySaleId() → findBySaleIdOrderByCreatedAtDesc()
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1))
                    .thenReturn(List.of(cashPayment));
            when(cashShiftRepository.save(any())).thenAnswer(inv -> {
                CashShift s = inv.getArgument(0);
                assertThat(s.getStatus()).isEqualTo("CLOSED");
                assertThat(s.getDifferenceAmount()).isEqualTo(0);
                return s;
            });
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            CashShiftDTO result = cashShiftService.closeShift(1, input);

            assertThat(result.getStatus()).isEqualTo("CLOSED");
        }

        @Test
        @DisplayName("Chot ca - chenh lech vuot nguong 500k - status PENDING_ADMIN_REVIEW")
        void closeShift_aboveThreshold_statusPendingReview() {
            CashShiftDTO input = new CashShiftDTO();
            input.setActualCashAmount(2_000_000);

            when(cashShiftRepository.findById(1)).thenReturn(Optional.of(openShift));
            when(saleRepository.findByBranchIdOrderBySaleDateDesc(1)).thenReturn(List.of());
            when(cashShiftRepository.save(any())).thenAnswer(inv -> {
                CashShift s = inv.getArgument(0);
                assertThat(s.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
                return s;
            });
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            CashShiftDTO result = cashShiftService.closeShift(1, input);

            assertThat(result.getStatus()).isEqualTo("PENDING_ADMIN_REVIEW");
        }

        @Test
        @DisplayName("Chot ca khong o OPEN - nem exception")
        void closeShift_nonOpenShift_throwsException() {
            openShift.setStatus("CLOSED");
            CashShiftDTO input = new CashShiftDTO();
            input.setActualCashAmount(500_000);

            when(cashShiftRepository.findById(1)).thenReturn(Optional.of(openShift));

            assertThatThrownBy(() -> cashShiftService.closeShift(1, input))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("OPEN");
        }

        @Test
        @DisplayName("Chot ca - chi tinh tien mat CASH, bo qua ONLINE")
        void closeShift_onlyCountsCashPayments() {
            Sale cashSale = new Sale();
            cashSale.setSaleId(1);
            cashSale.setStatus("COMPLETED");
            cashSale.setFinalAmount(50_000);
            cashSale.setSaleDate(LocalDateTime.now());

            Sale onlineSale = new Sale();
            onlineSale.setSaleId(2);
            onlineSale.setStatus("COMPLETED");
            onlineSale.setFinalAmount(90_000);
            onlineSale.setSaleDate(LocalDateTime.now());

            Payment cashPay = new Payment();
            cashPay.setPaymentMethod("CASH");
            cashPay.setStatus("PAID");

            Payment onlinePay = new Payment();
            onlinePay.setPaymentMethod("ONLINE");
            onlinePay.setStatus("PAID");

            CashShiftDTO input = new CashShiftDTO();
            input.setActualCashAmount(550_000); // 500000 + 50000 (chi CASH)

            when(cashShiftRepository.findById(1)).thenReturn(Optional.of(openShift));
            when(saleRepository.findByBranchIdOrderBySaleDateDesc(1))
                    .thenReturn(List.of(cashSale, onlineSale));
            // FIX: findBySaleId(1) → findBySaleIdOrderByCreatedAtDesc(1)
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(1))
                    .thenReturn(List.of(cashPay));
            // FIX: findBySaleId(2) → findBySaleIdOrderByCreatedAtDesc(2)
            when(paymentRepository.findBySaleIdOrderByCreatedAtDesc(2))
                    .thenReturn(List.of(onlinePay));
            when(cashShiftRepository.save(any())).thenAnswer(inv -> {
                CashShift s = inv.getArgument(0);
                assertThat(s.getSystemCashAmount()).isEqualTo(550_000);
                assertThat(s.getDifferenceAmount()).isEqualTo(0);
                assertThat(s.getStatus()).isEqualTo("CLOSED");
                return s;
            });
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            cashShiftService.closeShift(1, input);

            verify(cashShiftRepository).save(argThat(s -> s.getSystemCashAmount() == 550_000));
        }
    }

    // ===========================================================
    // confirmShift
    // ===========================================================

    @Nested
    @DisplayName("confirmShift")
    class ConfirmShift {

        @Test
        @DisplayName("Manager xac nhan ca - status doi sang CONFIRMED")
        void confirmShift_success() {
            openShift.setStatus("CLOSED");

            when(cashShiftRepository.findById(1)).thenReturn(Optional.of(openShift));
            when(cashShiftRepository.save(any())).thenAnswer(inv -> {
                CashShift s = inv.getArgument(0);
                assertThat(s.getStatus()).isEqualTo("CONFIRMED");
                assertThat(s.getManagerConfirmedBy()).isEqualTo(2);
                assertThat(s.getManagerConfirmedAt()).isNotNull();
                return s;
            });
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());
            when(appUserRepository.findById(2)).thenReturn(Optional.empty());

            CashShiftDTO result = cashShiftService.confirmShift(1, 2);

            assertThat(result.getStatus()).isEqualTo("CONFIRMED");
            verify(cashShiftRepository).save(argThat(s ->
                    "CONFIRMED".equals(s.getStatus()) && s.getManagerConfirmedBy() == 2));
        }
    }

    // ===========================================================
    // getOpenShift
    // ===========================================================

    @Test
    @DisplayName("getOpenShift - tim thay ca dang mo")
    void getOpenShift_found() {
        when(cashShiftRepository.findByPharmacistIdAndStatus(5, "OPEN"))
                .thenReturn(Optional.of(openShift));
        when(appUserRepository.findById(5)).thenReturn(Optional.empty());

        Optional<CashShiftDTO> result = cashShiftService.getOpenShift(5);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("getOpenShift - khong co ca nao dang mo")
    void getOpenShift_notFound() {
        when(cashShiftRepository.findByPharmacistIdAndStatus(5, "OPEN"))
                .thenReturn(Optional.empty());

        assertThat(cashShiftService.getOpenShift(5)).isEmpty();
    }
}