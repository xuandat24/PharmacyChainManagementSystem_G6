package fu.se2033.pharmacy.service.impl;

import fu.se2033.pharmacy.service.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    private final JdbcTemplate jdbcTemplate;

    public ReportServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> revenueReport(LocalDate fromDate, LocalDate toDate, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.branch_name,
                       CAST(p.paid_at AS DATE) AS report_date,
                       p.payment_method,
                       COUNT(p.payment_id) AS payment_count,
                       ISNULL(SUM(p.amount), 0) AS total_amount
                FROM payments p
                JOIN sales s ON p.sale_id = s.sale_id
                JOIN branches b ON s.branch_id = b.branch_id
                WHERE p.status = 'PAID'
                  AND CAST(p.paid_at AS DATE) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(fromDate);
        args.add(toDate);
        if (branchId != null) {
            sql.append(" AND s.branch_id = ? ");
            args.add(branchId);
        }
        sql.append("""
                GROUP BY b.branch_name, CAST(p.paid_at AS DATE), p.payment_method
                ORDER BY report_date DESC, b.branch_name
                """);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Override
    public List<Map<String, Object>> expenseReport(LocalDate fromDate, LocalDate toDate, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.branch_name,
                       e.expense_date,
                       e.expense_type,
                       e.status,
                       COUNT(e.expense_voucher_id) AS voucher_count,
                       ISNULL(SUM(e.amount), 0) AS total_amount
                FROM expense_vouchers e
                JOIN branches b ON e.branch_id = b.branch_id
                WHERE e.expense_date BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(fromDate);
        args.add(toDate);
        if (branchId != null) {
            sql.append(" AND e.branch_id = ? ");
            args.add(branchId);
        }
        sql.append("""
                GROUP BY b.branch_name, e.expense_date, e.expense_type, e.status
                ORDER BY e.expense_date DESC, b.branch_name
                """);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }

    @Override
    public List<Map<String, Object>> inventoryMovementReport(LocalDate fromDate, LocalDate toDate, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.branch_name,
                       m.medicine_name,
                       it.transaction_type,
                       COUNT(it.inventory_transaction_id) AS transaction_count,
                       ISNULL(SUM(it.quantity), 0) AS total_quantity
                FROM inventory_transactions it
                JOIN branches b ON it.branch_id = b.branch_id
                JOIN medicines m ON it.medicine_id = m.medicine_id
                WHERE CAST(it.created_at AS DATE) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(fromDate);
        args.add(toDate);
        if (branchId != null) {
            sql.append(" AND it.branch_id = ? ");
            args.add(branchId);
        }
        sql.append("""
                GROUP BY b.branch_name, m.medicine_name, it.transaction_type
                ORDER BY b.branch_name, m.medicine_name
                """);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray());
    }
}
