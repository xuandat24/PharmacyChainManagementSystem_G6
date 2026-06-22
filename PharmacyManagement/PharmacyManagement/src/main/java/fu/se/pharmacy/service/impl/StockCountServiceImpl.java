package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.StockCountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StockCountServiceImpl implements StockCountService {

    @Autowired
    private StockCountRepository stockCountRepository;

    @Autowired
    private StockCountDetailRepository detailRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    // Hạn mức chênh lệch kiểm kê (500.000 VNĐ)
    private static final BigDecimal STOCK_VARIANCE_LIMIT = new BigDecimal("500000.00");

    @Override
    public StockCount getStockCountById(Integer id) {
        return stockCountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu kiểm kê ID: " + id));
    }

    @Override
    public List<StockCount> getStockCountsByBranch(Integer branchId) {
        return stockCountRepository.findByBranch_BranchId(branchId);
    }

    @Override
    public List<StockCount> getPendingApprovalStockCounts() {
        return stockCountRepository.findByStatus("PENDING_ADMIN_APPROVAL");
    }

    @Override
    @Transactional
    public StockCount createStockCount(Integer branchId, Integer creatorId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi nhánh ID: " + branchId));

        Employee creator = employeeRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên ID: " + creatorId));

        StockCount count = new StockCount();
        count.setBranch(branch);
        count.setCreatedBy(creator);
        count.setCountDate(LocalDateTime.now());
        count.setStatus("DRAFT");

        // Chụp số liệu tồn kho thực tế của chi nhánh
        List<Inventory> activeInventories = inventoryRepository.findByBranch_BranchId(branchId);
        List<StockCountDetail> details = new ArrayList<>();

        for (Inventory inv : activeInventories) {
            if (inv.getQuantity() > 0) {
                StockCountDetail detail = new StockCountDetail();
                detail.setStockCount(count);
                detail.setMedicine(inv.getMedicine());
                detail.setBatchNumber(inv.getBatchNumber());
                detail.setSystemQuantity(inv.getQuantity());
                detail.setActualQuantity(inv.getQuantity()); // Mặc định để sửa sau
                detail.setDifference(0);
                details.add(detail);
            }
        }

        count.setDetails(details);
        return stockCountRepository.save(count);
    }

    @Override
    @Transactional
    public StockCount saveStockCount(StockCount count) {
        if (count.getDetails() != null) {
            for (StockCountDetail detail : count.getDetails()) {
                detail.setStockCount(count);
                detail.setDifference(detail.getActualQuantity() - detail.getSystemQuantity());
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

        BigDecimal totalVarianceValue = BigDecimal.ZERO;

        for (StockCountDetail detail : count.getDetails()) {
            detail.setDifference(detail.getActualQuantity() - detail.getSystemQuantity());
            detailRepository.save(detail);

            // Tìm giá mua của lô hàng để tính giá trị chênh lệch
            Optional<Inventory> invOpt = inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(
                    count.getBranch().getBranchId(),
                    detail.getMedicine().getMedicineId(),
                    detail.getBatchNumber()
            );

            BigDecimal price = invOpt.isPresent() && invOpt.get().getPurchasePrice() != null
                    ? invOpt.get().getPurchasePrice()
                    : detail.getMedicine().getUnitPrice(); // Dự phòng giá bán nếu thiếu giá mua

            BigDecimal varianceValue = price.multiply(BigDecimal.valueOf(Math.abs(detail.getDifference())));
            totalVarianceValue = totalVarianceValue.add(varianceValue);
        }

        // Kiểm tra hạn mức chênh lệch kiểm kê
        if (totalVarianceValue.compareTo(STOCK_VARIANCE_LIMIT) > 0) {
            // Vượt hạn mức -> PENDING ADMIN
            count.setStatus("PENDING_ADMIN_APPROVAL");
            stockCountRepository.save(count);
        } else {
            // Trong hạn mức -> Tự động COMPLETED và áp dụng chênh lệch vào kho
            count.setStatus("COMPLETED");
            stockCountRepository.save(count);
            inventoryService.applyStockCount(stockCountId);
        }
    }

    @Override
    @Transactional
    public void approveStockCount(Integer stockCountId, Integer adminId, String adminNotes) {
        StockCount count = getStockCountById(stockCountId);
        if (!"PENDING_ADMIN_APPROVAL".equals(count.getStatus())) {
            throw new IllegalStateException("Chỉ đợt kiểm kê chờ phê duyệt chênh lệch mới được duyệt.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Admin ID: " + adminId));

        count.setApprovedBy(admin);
        count.setApprovalDate(LocalDateTime.now());
        count.setNotes(adminNotes);
        count.setStatus("COMPLETED");
        stockCountRepository.save(count);

        // Áp dụng tồn vào kho thực tế
        inventoryService.applyStockCount(stockCountId);
    }

    @Override
    @Transactional
    public void rejectStockCount(Integer stockCountId, Integer adminId, String adminNotes) {
        StockCount count = getStockCountById(stockCountId);
        if (!"PENDING_ADMIN_APPROVAL".equals(count.getStatus())) {
            throw new IllegalStateException("Chỉ đợt kiểm kê chờ phê duyệt chênh lệch mới được phép bác bỏ.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Admin ID: " + adminId));

        count.setApprovedBy(admin);
        count.setApprovalDate(LocalDateTime.now());
        count.setNotes(adminNotes);
        count.setStatus("CANCELLED");
        stockCountRepository.save(count);
    }
}
