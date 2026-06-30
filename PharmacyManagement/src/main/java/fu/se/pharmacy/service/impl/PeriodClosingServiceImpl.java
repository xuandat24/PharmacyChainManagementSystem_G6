package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.enums.AccountingPeriodStatus;
import fu.se.pharmacy.dto.PeriodLockRequest;
import fu.se.pharmacy.dto.PeriodUnlockRequest;
import fu.se.pharmacy.entity.AccountingPeriod;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.exception.ResourceNotFoundException;
import fu.se.pharmacy.repository.AccountingPeriodRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.PeriodClosingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PeriodClosingServiceImpl implements PeriodClosingService {
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AuditLogService auditLogService;

    public PeriodClosingServiceImpl(AccountingPeriodRepository accountingPeriodRepository,
                                    AuditLogService auditLogService) {
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AccountingPeriod lockPeriod(PeriodLockRequest request) {
        AccountingPeriod period = accountingPeriodRepository
                .findByPeriodYearAndPeriodMonth(request.getYear(), request.getMonth())
                .orElseGet(() -> {
                    AccountingPeriod p = new AccountingPeriod();
                    p.setPeriodYear(request.getYear());
                    p.setPeriodMonth(request.getMonth());
                    p.setStatus(AccountingPeriodStatus.OPEN);
                    return p;
                });

        if (period.getStatus() == AccountingPeriodStatus.LOCKED) {
            throw new BusinessException("Kỳ này đã được khóa");
        }
        period.setStatus(AccountingPeriodStatus.LOCKED);
        period.setLockedBy(request.getLockedBy());
        period.setLockedAt(LocalDateTime.now());
        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(request.getLockedBy(), null, "LOCK_PERIOD", "AccountingPeriod", saved.getAccountingPeriodId(),
                "OPEN", "LOCKED", "Khóa sổ tháng");
        return saved;
    }

    @Override
    @Transactional
    public AccountingPeriod unlockPeriod(Integer periodId, PeriodUnlockRequest request) {
        AccountingPeriod period = accountingPeriodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỳ kế toán"));
        if (period.getStatus() == AccountingPeriodStatus.OPEN) {
            throw new BusinessException("Kỳ này đang mở, không cần mở khóa");
        }
        period.setStatus(AccountingPeriodStatus.OPEN);
        period.setUnlockReason(request.getReason());
        AccountingPeriod saved = accountingPeriodRepository.save(period);
        auditLogService.log(request.getUnlockedBy(), null, "UNLOCK_PERIOD", "AccountingPeriod", saved.getAccountingPeriodId(),
                "LOCKED", "OPEN", request.getReason());
        return saved;
    }

    @Override
    public boolean isDateLocked(LocalDate date) {
        return accountingPeriodRepository.findByPeriodYearAndPeriodMonth(date.getYear(), date.getMonthValue())
                .map(p -> p.getStatus() == AccountingPeriodStatus.LOCKED)
                .orElse(false);
    }
}
