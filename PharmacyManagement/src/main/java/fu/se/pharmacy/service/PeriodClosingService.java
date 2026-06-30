package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.PeriodLockRequest;
import fu.se.pharmacy.dto.PeriodUnlockRequest;
import fu.se.pharmacy.entity.AccountingPeriod;

import java.time.LocalDate;

public interface PeriodClosingService {
    AccountingPeriod lockPeriod(PeriodLockRequest request);
    AccountingPeriod unlockPeriod(Integer periodId, PeriodUnlockRequest request);
    boolean isDateLocked(LocalDate date);
}
