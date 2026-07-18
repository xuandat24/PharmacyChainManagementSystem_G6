package fu.se.pharmacy;

import fu.se.pharmacy.common.enums.AccountingPeriodStatus;
import fu.se.pharmacy.dto.PeriodLockRequest;
import fu.se.pharmacy.dto.PeriodUnlockRequest;
import fu.se.pharmacy.entity.AccountingPeriod;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.repository.AccountingPeriodRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.impl.PeriodClosingServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho PeriodClosingServiceImpl.
 * Business rules từ RDS (UC-36):
 *   BR-PR-01: Sau khi khóa, không tạo/sửa giao dịch thuộc kỳ đó
 *   BR-PR-02: Không khóa kỳ đã bị khóa
 *   BR-PR-03: Admin phải cung cấp lý do khi mở khóa
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PeriodClosingService - Unit Tests")
class PeriodClosingServiceTest {

    @Mock private AccountingPeriodRepository accountingPeriodRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private PeriodClosingServiceImpl periodClosingService;

    // ===========================================================
    // lockPeriod
    // ===========================================================

    @Nested
    @DisplayName("lockPeriod")
    class LockPeriod {

        @Test
        @DisplayName("Khoa ky chua ton tai - tao moi va khoa thanh cong")
        void lock_nonExistingPeriod_createsAndLocks() {
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2026, 6))
                    .thenReturn(Optional.empty());
            when(accountingPeriodRepository.save(any())).thenAnswer(inv -> {
                AccountingPeriod p = inv.getArgument(0);
                p.setAccountingPeriodId(1);
                return p;
            });
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            AccountingPeriod result = periodClosingService.lockPeriod(lockRequest(2026, 6, 1));

            assertThat(result.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
            assertThat(result.getLockedBy()).isEqualTo(1);
        }

        @Test
        @DisplayName("Khoa ky dang OPEN - status LOCKED")
        void lock_openPeriod_statusLocked() {
            AccountingPeriod existing = openPeriod(2026, 5);
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2026, 5))
                    .thenReturn(Optional.of(existing));
            when(accountingPeriodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            AccountingPeriod result = periodClosingService.lockPeriod(lockRequest(2026, 5, 1));

            assertThat(result.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
        }

        @Test
        @DisplayName("Khoa ky da bi khoa - nem exception (BR-PR-02)")
        void lock_alreadyLocked_throwsException() {
            AccountingPeriod locked = openPeriod(2026, 5);
            locked.setStatus(AccountingPeriodStatus.LOCKED);
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2026, 5))
                    .thenReturn(Optional.of(locked));

            assertThatThrownBy(() -> periodClosingService.lockPeriod(lockRequest(2026, 5, 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("khóa");

            verify(accountingPeriodRepository, never()).save(any());
        }
    }

    // ===========================================================
    // unlockPeriod
    // ===========================================================

    @Nested
    @DisplayName("unlockPeriod")
    class UnlockPeriod {

        @Test
        @DisplayName("Mo khoa ky LOCKED - status OPEN, ghi ly do (BR-PR-03)")
        void unlock_locked_statusOpen() {
            AccountingPeriod locked = openPeriod(2026, 5);
            locked.setAccountingPeriodId(1);
            locked.setStatus(AccountingPeriodStatus.LOCKED);

            when(accountingPeriodRepository.findById(1)).thenReturn(Optional.of(locked));
            when(accountingPeriodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            PeriodUnlockRequest req = new PeriodUnlockRequest();
            req.setUnlockedBy(1);
            req.setReason("Kiem tra so lieu lai");

            AccountingPeriod result = periodClosingService.unlockPeriod(1, req);

            assertThat(result.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
            assertThat(result.getUnlockReason()).isEqualTo("Kiem tra so lieu lai");
        }

        @Test
        @DisplayName("Mo khoa ky dang OPEN - nem exception")
        void unlock_alreadyOpen_throwsException() {
            AccountingPeriod open = openPeriod(2026, 5);
            open.setAccountingPeriodId(1);

            when(accountingPeriodRepository.findById(1)).thenReturn(Optional.of(open));

            PeriodUnlockRequest req = new PeriodUnlockRequest();
            req.setUnlockedBy(1);
            req.setReason("Test");

            assertThatThrownBy(() -> periodClosingService.unlockPeriod(1, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mở");
        }
    }

    // ===========================================================
    // isDateLocked
    // ===========================================================

    @Nested
    @DisplayName("isDateLocked")
    class IsDateLocked {

        @Test
        @DisplayName("Ngay thuoc ky da khoa - tra ve true")
        void isDateLocked_lockedPeriod_returnsTrue() {
            AccountingPeriod locked = openPeriod(2026, 5);
            locked.setStatus(AccountingPeriodStatus.LOCKED);
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2026, 5))
                    .thenReturn(Optional.of(locked));

            assertThat(periodClosingService.isDateLocked(LocalDate.of(2026, 5, 15))).isTrue();
        }

        @Test
        @DisplayName("Ngay thuoc ky chua khoa - tra ve false")
        void isDateLocked_openPeriod_returnsFalse() {
            AccountingPeriod open = openPeriod(2026, 6);
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2026, 6))
                    .thenReturn(Optional.of(open));

            assertThat(periodClosingService.isDateLocked(LocalDate.of(2026, 6, 1))).isFalse();
        }

        @Test
        @DisplayName("Ky chua ton tai - tra ve false (mac dinh mo)")
        void isDateLocked_noPeriod_returnsFalse() {
            when(accountingPeriodRepository.findByPeriodYearAndPeriodMonth(2025, 12))
                    .thenReturn(Optional.empty());

            assertThat(periodClosingService.isDateLocked(LocalDate.of(2025, 12, 31))).isFalse();
        }
    }

    // Helpers
    private PeriodLockRequest lockRequest(int year, int month, int lockedBy) {
        PeriodLockRequest req = new PeriodLockRequest();
        req.setYear(year);
        req.setMonth(month);
        req.setLockedBy(lockedBy);
        return req;
    }

    private AccountingPeriod openPeriod(int year, int month) {
        AccountingPeriod p = new AccountingPeriod();
        p.setPeriodYear(year);
        p.setPeriodMonth(month);
        p.setStatus(AccountingPeriodStatus.OPEN);
        return p;
    }
}
