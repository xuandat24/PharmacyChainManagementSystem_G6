package fu.se2033.pharmacy.service.impl;

import fu.se2033.pharmacy.common.enums.ExpenseVoucherStatus;
import fu.se2033.pharmacy.common.enums.StockTransferStatus;
import fu.se2033.pharmacy.dto.flow4.DashboardSummaryResponse;
import fu.se2033.pharmacy.repository.ExpenseVoucherRepository;
import fu.se2033.pharmacy.repository.StockTransferRepository;
import fu.se2033.pharmacy.service.DashboardService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {
    private final JdbcTemplate jdbcTemplate;
    private final StockTransferRepository stockTransferRepository;
    private final ExpenseVoucherRepository expenseVoucherRepository;

    public DashboardServiceImpl(JdbcTemplate jdbcTemplate,
                                StockTransferRepository stockTransferRepository,
                                ExpenseVoucherRepository expenseVoucherRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.stockTransferRepository = stockTransferRepository;
        this.expenseVoucherRepository = expenseVoucherRepository;
    }

    @Override
    public DashboardSummaryResponse getTodaySummary(Integer branchId) {
        LocalDate today = LocalDate.now();
        DashboardSummaryResponse response = new DashboardSummaryResponse();

        response.setTodayRevenue(queryPaymentAmount(today, branchId, null));
        response.setTodayCashRevenue(queryPaymentAmount(today, branchId, "CASH"));
        response.setTodayOnlineRevenue(queryPaymentAmount(today, branchId, "ONLINE"));
        response.setTodaySalesCount(queryTodaySalesCount(today, branchId));
        response.setTodayGoodsReceiptAmount(queryTodayGoodsReceiptAmount(today, branchId));
        response.setTodayExpenseAmount(queryTodayExpenseAmount(today, branchId));
        response.setLowStockCount(queryLowStockCount(branchId));
        response.setNearExpiryCount(queryNearExpiryCount(branchId));
        response.setPendingTransferCount(stockTransferRepository.countByStatus(StockTransferStatus.PENDING_ADMIN_APPROVAL));
        response.setPendingExpenseCount(expenseVoucherRepository.countByStatus(ExpenseVoucherStatus.SUBMITTED));
        return response;
    }

    private Integer queryPaymentAmount(LocalDate today, Integer branchId, String paymentMethod) {
        StringBuilder sql = new StringBuilder("""
                SELECT ISNULL(SUM(p.amount), 0)
                FROM payments p
                JOIN sales s ON p.sale_id = s.sale_id
                WHERE p.status = 'PAID'
                  AND CAST(p.paid_at AS DATE) = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(today);
        if (paymentMethod != null) {
            sql.append(" AND p.payment_method = ? ");
            args.add(paymentMethod);
        }
        if (branchId != null) {
            sql.append(" AND s.branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryTodaySalesCount(LocalDate today, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM sales
                WHERE status = 'COMPLETED'
                  AND CAST(sale_date AS DATE) = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(today);
        if (branchId != null) {
            sql.append(" AND branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryTodayGoodsReceiptAmount(LocalDate today, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT ISNULL(SUM(total_actual_amount), 0)
                FROM goods_receipts
                WHERE status = 'POSTED'
                  AND CAST(posted_at AS DATE) = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(today);
        if (branchId != null) {
            sql.append(" AND branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryTodayExpenseAmount(LocalDate today, Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT ISNULL(SUM(amount), 0)
                FROM expense_vouchers
                WHERE status IN ('APPROVED', 'PAID')
                  AND expense_date = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(today);
        if (branchId != null) {
            sql.append(" AND branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryLowStockCount(Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM inventory_batches ib
                JOIN medicines m ON ib.medicine_id = m.medicine_id
                WHERE ib.status = 'AVAILABLE'
                  AND ib.quantity_on_hand <= m.min_stock_level
                """);
        List<Object> args = new ArrayList<>();
        if (branchId != null) {
            sql.append(" AND ib.branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryNearExpiryCount(Integer branchId) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM inventory_batches ib
                WHERE ib.status = 'AVAILABLE'
                  AND ib.quantity_on_hand > 0
                  AND ib.expiry_date BETWEEN CAST(GETDATE() AS DATE) AND DATEADD(DAY, 60, CAST(GETDATE() AS DATE))
                """);
        List<Object> args = new ArrayList<>();
        if (branchId != null) {
            sql.append(" AND ib.branch_id = ? ");
            args.add(branchId);
        }
        return queryInt(sql.toString(), args);
    }

    private Integer queryInt(String sql, List<Object> args) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        return result == null ? 0 : result;
    }
}
