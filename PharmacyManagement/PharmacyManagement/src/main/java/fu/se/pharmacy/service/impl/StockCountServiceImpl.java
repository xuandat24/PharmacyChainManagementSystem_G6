package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.StockCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockCountServiceImpl implements StockCountService {

    @Autowired private StockCountRepository stockCountRepository;
    @Autowired private StockCountDetailRepository detailRepository;
    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private InventoryService inventoryService;

    private static final int STOCK_VARIANCE_LIMIT = 500_000;

    @Override
    public StockCount getStockCountById(Integer id) {
        return stockCountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + id));
    }

    @Override
    public List<StockCount> getStockCountsByBranch(Integer branchId) {
        return stockCountRepository.findByBranchIdOrderByCountDateDesc(branchId);
    }

    @Override
    public List<StockCount> getPendingApprovalStockCounts() {
        return stockCountRepository.findByStatus("SUBMITTED");
    }

    // FIX: Admin dùng getAllStockCounts() thay vì getStockCountsByBranch(null)
    @Override
    public List<StockCount> getAllStockCounts() {
        return stockCountRepository.findAll();
    }

    @Override
    @Transactional
    public StockCount createStockCount(Integer branchId, Integer creatorId) {
        StockCount count = new StockCount();
        count.setBranchId(branchId);
        count.setCreatedBy(creatorId);
        count.setCountDate(LocalDateTime.now());
        count.setStatus("DRAFT");

        List<InventoryBatch> batches = inventoryBatchRepository.findAll().stream()
                .filter(b -> b.getBranchId().equals(branchId)
                        && "AVAILABLE".equals(b.getStatus())
                        && b.getQuantityOnHand() != null && b.getQuantityOnHand() > 0)
                .toList();

        StockCount saved = stockCountRepository.save(count);

        List<StockCountDetail> details = new ArrayList<>();
        for (InventoryBatch batch : batches) {
            StockCountDetail detail = new StockCountDetail();
            detail.setStockCountId(saved.getStockCountId());
            detail.setInventoryBatchId(batch.getInventoryBatchId());
            detail.setSystemQuantity(batch.getQuantityOnHand());
            detail.setActualQuantity(batch.getQuantityOnHand());
            details.add(detailRepository.save(detail));
        }
        saved.setDetails(details);
        return saved;
    }

    @Override
    @Transactional
    public StockCount saveStockCount(StockCount count) {
        if (count.getDetails() != null) {
            for (StockCountDetail detail : count.getDetails()) {
                detail.setStockCountId(count.getStockCountId());
                detailRepository.save(detail);
            }
        }
        return stockCountRepository.save(count);
    }

    @Override
    @Transactional
    public void submitStockCount(Integer stockCountId) {
        StockCount count = getStockCountById(stockCountId);
        if (!"DRAFT".equals(count.getStatus())) {
            throw new IllegalStateException("Đợt kiểm kê đã được gửi duyệt hoặc đã hoàn tất.");
        }

        List<StockCountDetail> details = detailRepository.findByStockCountId(stockCountId);
        int totalVarianceValue = 0;

        for (StockCountDetail detail : details) {
            int diff = Math.abs(detail.getDifference());
            if (diff > 0) {
                int batchCost = inventoryBatchRepository.findById(detail.getInventoryBatchId())
                        .map(b -> b.getUnitCost() != null ? b.getUnitCost() : 0)
                        .orElse(0);
                totalVarianceValue += diff * batchCost;
            }
        }

        if (totalVarianceValue > STOCK_VARIANCE_LIMIT) {
            count.setStatus("SUBMITTED"); // chờ Admin duyệt
        } else {
            count.setStatus("POSTED");   // tự động hoàn tất
            inventoryService.applyStockCount(stockCountId);
        }
        count.setTotalVarianceAmount(totalVarianceValue);
        stockCountRepository.save(count);
    }

    @Override
    @Transactional
    public void approveStockCount(Integer stockCountId, Integer adminId, String adminNotes) {
        StockCount count = getStockCountById(stockCountId);
        if (!"SUBMITTED".equals(count.getStatus())) {
            throw new IllegalStateException("Chỉ đợt kiểm kê chờ duyệt mới được phê duyệt.");
        }
        count.setApprovedBy(adminId);
        count.setApprovedAt(LocalDateTime.now());
        count.setNote(adminNotes);
        count.setStatus("APPROVED");
        stockCountRepository.save(count);
        inventoryService.applyStockCount(stockCountId);
    }

    @Override
    @Transactional
    public void rejectStockCount(Integer stockCountId, Integer adminId, String adminNotes) {
        StockCount count = getStockCountById(stockCountId);
        if (!"SUBMITTED".equals(count.getStatus())) {
            throw new IllegalStateException("Chỉ đợt kiểm kê chờ duyệt mới được bác bỏ.");
        }
        count.setApprovedBy(adminId);
        count.setApprovedAt(LocalDateTime.now());
        count.setNote(adminNotes);
        count.setStatus("CANCELLED");
        stockCountRepository.save(count);
    }
}