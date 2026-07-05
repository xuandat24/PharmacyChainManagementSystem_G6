package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.GoodsReceiptService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.PeriodClosingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    @Autowired private GoodsReceiptRepository goodsReceiptRepository;
    @Autowired private GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    @Autowired private PurchaseRequestRepository purchaseRequestRepository;
    @Autowired private PurchaseRequestDetailRepository purchaseRequestDetailRepository;
    @Autowired private InventoryService inventoryService;
    // FIX: kiểm tra kỳ kế toán đã khóa trước khi ghi nhận nhập kho + ghi audit log
    @Autowired private PeriodClosingService periodClosingService;
    @Autowired private AuditLogService auditLogService;

    // FIX: Bỏ EmployeeRepository, dùng Integer FK trực tiếp

    @Override
    public GoodsReceipt getReceiptById(Integer id) {
        return goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phieu nhan hang ID: " + id));
    }

    @Override
    public List<GoodsReceipt> getReceiptsByBranch(Integer branchId) {
        // FIX: findByBranch_BranchId → findByBranchId (entity dùng integer FK)
        return goodsReceiptRepository.findByBranchId(branchId);
    }

    @Override
    public List<GoodsReceipt> getAllReceipts() {
        return goodsReceiptRepository.findAll();
    }

    @Override
    public List<GoodsReceipt> getPendingApprovalReceipts() {
        return goodsReceiptRepository.findByStatus("PENDING_ADMIN_APPROVAL");
    }

    @Override
    @Transactional
    public GoodsReceipt saveReceipt(GoodsReceipt receipt) {
        // FIX: BigDecimal → Integer, bỏ setGoodsReceipt (entity dùng goodsReceiptId)
        int total = 0;
        if (receipt.getDetails() != null) {
            for (GoodsReceiptDetail detail : receipt.getDetails()) {
                // goodsReceiptId sẽ được set sau khi save receipt
                int lineTotal = (detail.getActualUnitPrice() != null ? detail.getActualUnitPrice() : 0)
                        * (detail.getReceivedQuantity() != null ? detail.getReceivedQuantity() : 0);
                total += lineTotal;
            }
        }
        receipt.setTotalActualAmount(total);

        // Save receipt trước để có ID
        GoodsReceipt saved = goodsReceiptRepository.save(receipt);

        // Gán goodsReceiptId vào từng detail và save
        if (saved.getDetails() != null) {
            for (GoodsReceiptDetail detail : saved.getDetails()) {
                detail.setGoodsReceiptId(saved.getReceiptId());
                goodsReceiptDetailRepository.save(detail);
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public void submitAndCheckVariance(Integer receiptId) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"DRAFT".equals(receipt.getStatus())) {
            throw new IllegalStateException("Phieu nhan hang da duoc gui hoac da xu ly truoc do.");
        }
        if (periodClosingService.isDateLocked(LocalDate.now())) {
            throw new IllegalStateException("Ky ke toan hien tai da bi khoa, khong the nhan hang.");
        }

        // FIX: receipt.getPurchaseRequest() → dùng purchaseRequestId integer FK
        PurchaseRequest pr = purchaseRequestRepository.findById(receipt.getPurchaseRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau nhap hang"));

        List<PurchaseRequestDetail> prDetails = purchaseRequestDetailRepository
                .findByPurchaseRequestId(pr.getPurchaseRequestId());

        // Map medicineId → PurchaseRequestDetail
        Map<Integer, PurchaseRequestDetail> prDetailMap = prDetails.stream()
                .collect(Collectors.toMap(PurchaseRequestDetail::getMedicineId, d -> d));

        List<GoodsReceiptDetail> grDetails = goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId);

        boolean hasVariance = false;

        for (GoodsReceiptDetail grDetail : grDetails) {
            PurchaseRequestDetail prDetail = prDetailMap.get(grDetail.getMedicineId());

            if (prDetail == null) {
                hasVariance = true;
                grDetail.setOrderedQuantity(0);
            } else {
                Integer approved = prDetail.getApprovedQuantity() != null ? prDetail.getApprovedQuantity() : 0;
                grDetail.setOrderedQuantity(approved);
                // FIX: BigDecimal.compareTo → Integer comparison
                if (grDetail.getReceivedQuantity() != null && grDetail.getReceivedQuantity() > approved) {
                    hasVariance = true;
                }
                if (grDetail.getActualUnitPrice() != null && prDetail.getExpectedUnitPrice() != null
                        && grDetail.getActualUnitPrice() > prDetail.getExpectedUnitPrice()) {
                    hasVariance = true;
                }
            }
            goodsReceiptDetailRepository.save(grDetail);
        }

        if (hasVariance) {
            receipt.setStatus("PENDING_ADMIN_APPROVAL");
        } else {
            receipt.setStatus("POSTED");
            receipt.setPostedAt(LocalDateTime.now());
            goodsReceiptRepository.save(receipt);
            inventoryService.postGoodsReceipt(receiptId);
            return;
        }
        goodsReceiptRepository.save(receipt);
    }

    @Override
    @Transactional
    public void approveVariance(Integer receiptId, Integer adminId, String adminNotes) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) {
            throw new IllegalStateException("Chi phieu nhan hang dang cho duyet moi duoc phep duyet.");
        }
        if (periodClosingService.isDateLocked(LocalDate.now())) {
            throw new IllegalStateException("Ky ke toan hien tai da bi khoa, khong the duyet nhan hang.");
        }

        // FIX: setApprovedBy(admin) → setApprovedBy(adminId integer)
        receipt.setApprovedBy(adminId);
        receipt.setPostedAt(LocalDateTime.now());
        receipt.setNote(adminNotes);
        receipt.setStatus("POSTED");
        goodsReceiptRepository.save(receipt);

        inventoryService.postGoodsReceipt(receiptId);

        auditLogService.log(adminId, receipt.getBranchId(), "GOODS_RECEIPT_APPROVE_VARIANCE",
                "GoodsReceipt", receiptId, "PENDING_ADMIN_APPROVAL", "POSTED", adminNotes);
    }

    @Override
    @Transactional
    public void rejectReceipt(Integer receiptId, Integer adminId, String adminNotes) {
        GoodsReceipt receipt = getReceiptById(receiptId);
        if (!"PENDING_ADMIN_APPROVAL".equals(receipt.getStatus())) {
            throw new IllegalStateException("Chi phieu nhan hang dang cho duyet moi duoc phep tu choi.");
        }

        receipt.setApprovedBy(adminId);
        receipt.setNote(adminNotes);
        receipt.setStatus("REJECTED");
        goodsReceiptRepository.save(receipt);

        auditLogService.log(adminId, receipt.getBranchId(), "GOODS_RECEIPT_REJECT",
                "GoodsReceipt", receiptId, "PENDING_ADMIN_APPROVAL", "REJECTED", adminNotes);
    }
}