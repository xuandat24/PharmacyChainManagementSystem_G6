package fu.se.pharmacy.entity;

import fu.se.pharmacy.common.enums.ExpenseType;
import fu.se.pharmacy.common.enums.ExpenseVoucherStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_vouchers")
public class ExpenseVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_voucher_id")
    private Integer expenseVoucherId;

    @Column(name = "expense_code", nullable = false, unique = true, length = 40)
    private String expenseCode;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_type", nullable = false, length = 40)
    private ExpenseType expenseType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "document_no", length = 80)
    private String documentNo;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExpenseVoucherStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public Integer getExpenseVoucherId() { return expenseVoucherId; }
    public void setExpenseVoucherId(Integer expenseVoucherId) { this.expenseVoucherId = expenseVoucherId; }
    public String getExpenseCode() { return expenseCode; }
    public void setExpenseCode(String expenseCode) { this.expenseCode = expenseCode; }
    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public ExpenseType getExpenseType() { return expenseType; }
    public void setExpenseType(ExpenseType expenseType) { this.expenseType = expenseType; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ExpenseVoucherStatus getStatus() { return status; }
    public void setStatus(ExpenseVoucherStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
