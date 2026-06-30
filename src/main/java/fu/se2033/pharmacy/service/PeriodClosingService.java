package fu.se2033.pharmacy.service;

import fu.se2033.pharmacy.dto.flow4.PeriodLockRequest;
import fu.se2033.pharmacy.dto.flow4.PeriodUnlockRequest;
import fu.se2033.pharmacy.entity.AccountingPeriod;

import java.time.LocalDate;

public interface PeriodClosingService {
    AccountingPeriod lockPeriod(PeriodLockRequest request);
    AccountingPeriod unlockPeriod(Integer periodId, PeriodUnlockRequest request);
    boolean isDateLocked(LocalDate date);
}
