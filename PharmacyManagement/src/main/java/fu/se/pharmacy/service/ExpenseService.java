package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.ExpenseVoucherCreateRequest;
import fu.se.pharmacy.entity.ExpenseVoucher;

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
