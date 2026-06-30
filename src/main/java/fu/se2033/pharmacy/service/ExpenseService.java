package fu.se2033.pharmacy.service;

import fu.se2033.pharmacy.dto.flow4.ApprovalRequest;
import fu.se2033.pharmacy.dto.flow4.ExpenseVoucherCreateRequest;
import fu.se2033.pharmacy.entity.ExpenseVoucher;

import java.util.List;

public interface ExpenseService {
    ExpenseVoucher createExpense(ExpenseVoucherCreateRequest request);
    ExpenseVoucher submitExpense(Integer expenseId);
    ExpenseVoucher approveExpense(Integer expenseId, ApprovalRequest request);
    ExpenseVoucher rejectExpense(Integer expenseId, ApprovalRequest request);
    ExpenseVoucher markPaid(Integer expenseId, Integer userId);
    ExpenseVoucher getById(Integer expenseId);
    List<ExpenseVoucher> getAll();
    List<ExpenseVoucher> getByBranch(Integer branchId);
}
