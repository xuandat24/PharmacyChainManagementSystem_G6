package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.PeriodClosingService;
import fu.se.pharmacy.service.StockCountService;
import fu.se.pharmacy.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockCountServiceImpl implements StockCountService {

    @Autowired private StockCountRepository stockCountRepository;
    @Autowired private StockCountDetailRepository detailRepository;
    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private InventoryService inventoryService;
    // FIX: đọc hạn mức từ system_settings (STOCK_VARIANCE_LIMIT) thay vì hardcode,
    // để Admin chỉnh trong màn /settings có tác dụng thật.
    @Autowired private SystemSettingService systemSettingService;
    @Autowired private AuditLogService auditLogService;
    @Autowired private PeriodClosingService periodClosingService;

    private static final BigDecimal DEFAULT_STOCK_VARIANCE_LIMIT = BigDecimal.valueOf(500_000);

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
        if (periodClosingService.isDateLocked(LocalDate.now())) {
            throw new IllegalStateException("Kỳ kế toán hiện tại đã bị khóa, không thể kiểm kê.");
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

        BigDecimal limit = systemSettingService.getMoneyLimit(SettingKeys.STOCK_VARIANCE_LIMIT, DEFAULT_STOCK_VARIANCE_LIMIT);
        if (BigDecimal.valueOf(totalVarianceValue).compareTo(limit) > 0) {
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
        if (periodClosingService.isDateLocked(LocalDate.now())) {
            throw new IllegalStateException("Kỳ kế toán hiện tại đã bị khóa, không thể duyệt kiểm kê.");
        }
        count.setApprovedBy(adminId);
        count.setApprovedAt(LocalDateTime.now());
        count.setNote(adminNotes);
        count.setStatus("APPROVED");
        stockCountRepository.save(count);
        inventoryService.applyStockCount(stockCountId);

        auditLogService.log(adminId, count.getBranchId(), "STOCK_COUNT_APPROVE", "StockCount",
                stockCountId, "SUBMITTED", "APPROVED", adminNotes);
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

        auditLogService.log(adminId, count.getBranchId(), "STOCK_COUNT_REJECT", "StockCount",
                stockCountId, "SUBMITTED", "CANCELLED", adminNotes);
    }
}