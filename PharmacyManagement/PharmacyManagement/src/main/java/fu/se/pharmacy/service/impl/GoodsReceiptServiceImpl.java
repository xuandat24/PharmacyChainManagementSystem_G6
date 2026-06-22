package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.GoodsReceiptService;
import fu.se.pharmacy.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    @Autowired
    private GoodsReceiptRepository goodsReceiptRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Override
    public GoodsReceipt getReceiptById(Integer id) {
        return goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu nhận hàng ID: " + id));
    }

    @Override
    public List<GoodsReceipt> getReceiptsByBranch(Integer branchId) {
        return goodsReceiptRepository.findByBranch_BranchId(branchId);
    }

    @Override
    public List<GoodsReceipt> getPendingApprovalReceipts() {
        return goodsReceiptRepository.findByStatus("PENDING_ADMIN_APPROVAL");
    }

    @Override
    @Transactional
    public GoodsReceipt saveReceipt(GoodsReceipt receipt) {
        if (receipt.getDetails() != null) {
            BigDecimal total = BigDecimal.ZERO;
            for (GoodsReceiptDetail detail : receipt.getDetails()) {
                detail.setGoodsReceipt(receipt);
                BigDecimal detailTotal = detail.getPurchasePrice().multiply(BigDecimal.valueOf(detail.getQuantityReceived()));
                total = total.add(detailTotal);
            }
            receipt.setTotalAmount(total);
        }
        return goodsReceiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void submitAndCheckVariance(Integer receiptId) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new IllegalStateException("Phiếu nhận hàng đã được gửi hoặc đã xử lý trước đó.");
        }

        PurchaseRequest pr = receipt.getPurchaseRequest();
        Map<Integer, PurchaseRequestDetail> prDetailMap = pr.getDetails().stream()
                .collect(Collectors.toMap(d -> d.getMedicine().getMedicineId(), d -> d));

        boolean hasVariance = false;

        for (GoodsReceiptDetail grDetail : receipt.getDetails()) {
            PurchaseRequestDetail prDetail = prDetailMap.get(grDetail.getMedicine().getMedicineId());
            
            if (prDetail == null) {
                // Nhận sai thuốc so với yêu cầu
                hasVariance = true;
                grDetail.setQuantityOrdered(0);
            } else {
                grDetail.setQuantityOrdered(prDetail.getQuantityApproved());
                // Kiểm tra 1: Nhận vượt số lượng được duyệt
                if (grDetail.getQuantityReceived() > prDetail.getQuantityApproved()) {
                    hasVariance = true;
                }
                // Kiểm tra 2: Giá mua cao hơn giá dự kiến
                if (grDetail.getPurchasePrice().compareTo(prDetail.getEstimatedPrice()) > 0) {
                    hasVariance = true;
                }
            }
        }

        if (hasVariance) {
            receipt.setStatus("PENDING_ADMIN_APPROVAL");
            goodsReceiptRepository.save(receipt);
        } else {
            // Không có chênh lệch -> Chuyển POSTED và thực hiện tăng kho
            receipt.setStatus("POSTED");
            goodsReceiptRepository.save(receipt);
            inventoryService.postGoodsReceipt(receiptId);
        }
    }

    @Override
    @Transactional
    public void approveVariance(Integer receiptId, Integer adminId, String adminNotes) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) {
            throw new IllegalStateException("Chỉ phiếu nhận hàng đang chờ duyệt chênh lệch mới được phép duyệt.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Admin ID: " + adminId));

        receipt.setApprovedBy(admin);
        receipt.setApprovalDate(LocalDateTime.now());
        receipt.setAdminNotes(adminNotes);
        receipt.setStatus("POSTED");
        goodsReceiptRepository.save(receipt);

        // Tăng tồn kho
        inventoryService.postGoodsReceipt(receiptId);
    }

    @Override
    @Transactional
    public void rejectReceipt(Integer receiptId, Integer adminId, String adminNotes) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) {
            throw new IllegalStateException("Chỉ phiếu nhận hàng đang chờ duyệt chênh lệch mới được phép từ chối.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Admin ID: " + adminId));

        receipt.setApprovedBy(admin);
        receipt.setApprovalDate(LocalDateTime.now());
        receipt.setAdminNotes(adminNotes);
        receipt.setStatus("REJECTED");
        goodsReceiptRepository.save(receipt);
    }
}
