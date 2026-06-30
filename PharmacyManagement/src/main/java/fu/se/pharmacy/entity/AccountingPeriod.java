package fu.se.pharmacy.entity;

import fu.se.pharmacy.common.enums.AccountingPeriodStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_periods", uniqueConstraints = @UniqueConstraint(name = "UQ_accounting_period", columnNames = {"period_year", "period_month"}))
public class AccountingPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accounting_period_id")
    private Integer accountingPeriodId;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountingPeriodStatus status;

    @Column(name = "locked_by")
    private Integer lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "unlock_reason", length = 500)
    private String unlockReason;

    public Integer getAccountingPeriodId() { return accountingPeriodId; }
    public void setAccountingPeriodId(Integer accountingPeriodId) { this.accountingPeriodId = accountingPeriodId; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public AccountingPeriodStatus getStatus() { return status; }
    public void setStatus(AccountingPeriodStatus status) { this.status = status; }
    public Integer getLockedBy() { return lockedBy; }
    public void setLockedBy(Integer lockedBy) { this.lockedBy = lockedBy; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
    public String getUnlockReason() { return unlockReason; }
    public void setUnlockReason(String unlockReason) { this.unlockReason = unlockReason; }
}
