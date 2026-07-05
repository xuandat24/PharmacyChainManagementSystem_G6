package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_shifts")
@Data
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_shift_id")
    private Integer cashShiftId;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "pharmacist_id", nullable = false)
    private Integer pharmacistId;

    @Column(name = "opened_at", updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_cash_amount", nullable = false)
    private Integer openingCashAmount = 0;

    @Column(name = "system_cash_amount", nullable = false)
    private Integer systemCashAmount = 0;

    @Column(name = "actual_cash_amount")
    private Integer actualCashAmount;

    @Column(name = "difference_amount")
    private Integer differenceAmount;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "OPEN";

    @Column(name = "manager_confirmed_by")
    private Integer managerConfirmedBy;

    @Column(name = "manager_confirmed_at")
    private LocalDateTime managerConfirmedAt;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @PrePersist
    protected void onCreate() {
        if (this.openedAt == null) this.openedAt = LocalDateTime.now();
    }
}