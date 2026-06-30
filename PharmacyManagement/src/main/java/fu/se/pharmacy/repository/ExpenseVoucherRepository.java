package fu.se.pharmacy.repository;

import fu.se.pharmacy.common.enums.ExpenseVoucherStatus;
import fu.se.pharmacy.entity.ExpenseVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseVoucherRepository extends JpaRepository<ExpenseVoucher, Integer> {
    Optional<ExpenseVoucher> findByExpenseCode(String expenseCode);
    List<ExpenseVoucher> findByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<ExpenseVoucher> findByStatusOrderByCreatedAtDesc(ExpenseVoucherStatus status);
    int countByStatus(ExpenseVoucherStatus status);
    List<ExpenseVoucher> findByExpenseDateBetween(LocalDate from, LocalDate to);
}
