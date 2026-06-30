package fu.se2033.pharmacy.dto.flow4;

import fu.se2033.pharmacy.common.enums.ExpenseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ExpenseVoucherCreateRequest {
    @NotNull(message = "Chi nhánh không được để trống")
    private Integer branchId;

    @NotNull(message = "Người tạo phiếu không được để trống")
    private Integer createdBy;

    @NotNull(message = "Loại chi phí không được để trống")
    private ExpenseType expenseType;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private Integer amount;

    @NotNull(message = "Ngày chi không được để trống")
    private LocalDate expenseDate;

    @Size(max = 80, message = "Số chứng từ không được vượt quá 80 ký tự")
    private String documentNo;

    @NotBlank(message = "Nội dung chi không được để trống")
    @Size(max = 500, message = "Nội dung chi không được vượt quá 500 ký tự")
    private String description;

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
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
}
