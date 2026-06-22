package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.StockCount;
import java.util.List;

public interface StockCountService {
    StockCount getStockCountById(Integer id);
    List<StockCount> getStockCountsByBranch(Integer branchId);
    List<StockCount> getPendingApprovalStockCounts();
    StockCount createStockCount(Integer branchId, Integer creatorId);
    StockCount saveStockCount(StockCount count);
    void submitStockCount(Integer stockCountId);
    void approveStockCount(Integer stockCountId, Integer adminId, String adminNotes);
    void rejectStockCount(Integer stockCountId, Integer adminId, String adminNotes);
}
