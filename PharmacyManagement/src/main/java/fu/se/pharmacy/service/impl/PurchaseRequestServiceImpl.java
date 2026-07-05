package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.entity.PurchaseRequest;
import fu.se.pharmacy.entity.PurchaseRequestDetail;
import fu.se.pharmacy.repository.PurchaseRequestDetailRepository;
import fu.se.pharmacy.repository.PurchaseRequestRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.PurchaseRequestService;
import fu.se.pharmacy.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    @Autowired private PurchaseRequestRepository purchaseRequestRepository;
    @Autowired private PurchaseRequestDetailRepository detailRepository;
    @Autowired private AuditLogService auditLogService;
    // FIX: inject SystemSettingService để đọc PURCHASE_APPROVAL_LIMIT từ DB
    // thay vì hardcode hoặc bỏ qua hẳn việc kiểm tra hạn mức
    @Autowired private SystemSettingService systemSettingService;

    private static final BigDecimal DEFAULT_PURCHASE_LIMIT = BigDecimal.valueOf(5_000_000);

    @Override
    public List<PurchaseRequest> getAllRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    public List<PurchaseRequest> getRequestsByBranch(Integer branchId) {
        return purchaseRequestRepository.findByBranchIdOrderByRequestDateDesc(branchId);
    }

    @Override
    public PurchaseRequest getRequestById(Integer id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau nhap hang ID: " + id));
    }

    @Override
    @Transactional
    public PurchaseRequest saveRequest(PurchaseRequest request) {
        if (request.getRequestDate() == null) {
            request.setRequestDate(LocalDateTime.now());
        }
        int total = 0;
        if (request.getDetails() != null) {
            for (PurchaseRequestDetail d : request.getDetails()) {
                total += (d.getExpectedUnitPrice() != null ? d.getExpectedUnitPrice() : 0)
                        * (d.getRequestedQuantity() != null ? d.getRequestedQuantity() : 0);
            }
        }
        request.setTotalEstimatedAmount(total);

        // Lưu parent trước (không cascade detail cùng lúc tránh NULL FK)
        List<PurchaseRequestDetail> details = request.getDetails();
        request.setDetails(null);
        PurchaseRequest saved = purchaseRequestRepository.save(request);

        if (details != null) {
            for (PurchaseRequestDetail d : details) {
                d.setPurchaseRequestId(saved.getPurchaseRequestId());
                detailRepository.save(d);
            }
            saved.setDetails(details);
        }
        return saved;
    }

    @Override
    @Transactional
    public void submitRequest(Integer requestId) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"DRAFT".equals(request.getStatus())) {
            throw new IllegalStateException("Chi yeu cau dang o DRAFT moi duoc gui duyet.");
        }

        // FIX: đọc hạn mức từ system_settings thay vì không kiểm tra
        // Nếu tổng ước tính vượt PURCHASE_APPROVAL_LIMIT → PENDING_ADMIN_APPROVAL
        // để Admin phải duyệt trực tiếp thay vì BranchManager tự duyệt được
        BigDecimal limit = systemSettingService.getMoneyLimit(
                SettingKeys.PURCHASE_APPROVAL_LIMIT, DEFAULT_PURCHASE_LIMIT);
        int total = request.getTotalEstimatedAmount() != null ? request.getTotalEstimatedAmount() : 0;

        if (BigDecimal.valueOf(total).compareTo(limit) > 0) {
            request.setStatus("PENDING_ADMIN_APPROVAL");
        } else {
            request.setStatus("SUBMITTED");
        }
        purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void approveRequest(Integer requestId, Integer adminId, String adminNotes,
                               List<PurchaseRequestDetail> approvedDetails) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"SUBMITTED".equals(request.getStatus()) && !"PENDING_ADMIN_APPROVAL".equals(request.getStatus())) {
            throw new IllegalStateException("Chi yeu cau SUBMITTED hoac PENDING_ADMIN_APPROVAL moi duoc duyet.");
        }

        Map<Integer, Integer> approvedQtyMap = approvedDetails.stream()
                .filter(d -> d.getPurchaseRequestDetailId() != null)
                .collect(Collectors.toMap(
                        PurchaseRequestDetail::getPurchaseRequestDetailId,
                        d -> d.getApprovedQuantity() != null ? d.getApprovedQuantity() : 0));

        boolean fullyApproved = true;
        boolean hasSomeApproval = false;

        List<PurchaseRequestDetail> dbDetails = detailRepository.findByPurchaseRequestId(requestId);
        for (PurchaseRequestDetail detail : dbDetails) {
            Integer approvedQty = approvedQtyMap.getOrDefault(detail.getPurchaseRequestDetailId(), 0);
            int requested = detail.getRequestedQuantity() != null ? detail.getRequestedQuantity() : 0;
            if (approvedQty < 0 || approvedQty > requested) {
                throw new IllegalArgumentException("So luong duyet khong hop le cho medicine ID: " + detail.getMedicineId());
            }
            detail.setApprovedQuantity(approvedQty);
            detailRepository.save(detail);
            if (approvedQty < requested) fullyApproved = false;
            if (approvedQty > 0) hasSomeApproval = true;
        }

        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());
        request.setNote(adminNotes);
        request.setStatus(hasSomeApproval ? (fullyApproved ? "APPROVED" : "PARTIALLY_APPROVED") : "REJECTED");
        purchaseRequestRepository.save(request);

        auditLogService.log(adminId, request.getBranchId(), "PURCHASE_REQUEST_APPROVE",
                "PurchaseRequest", requestId, "SUBMITTED", request.getStatus(), adminNotes);
    }

    @Override
    @Transactional
    public void rejectRequest(Integer requestId, Integer adminId, String adminNotes) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"SUBMITTED".equals(request.getStatus()) && !"PENDING_ADMIN_APPROVAL".equals(request.getStatus())) {
            throw new IllegalStateException("Chi yeu cau SUBMITTED hoac PENDING_ADMIN_APPROVAL moi duoc tu choi.");
        }
        request.setApprovedBy(adminId);
        request.setApprovedAt(LocalDateTime.now());
        request.setNote(adminNotes);
        request.setStatus("REJECTED");
        detailRepository.findByPurchaseRequestId(requestId).forEach(d -> {
            d.setApprovedQuantity(0);
            detailRepository.save(d);
        });
        purchaseRequestRepository.save(request);
        auditLogService.log(adminId, request.getBranchId(), "PURCHASE_REQUEST_REJECT",
                "PurchaseRequest", requestId, "SUBMITTED", "REJECTED", adminNotes);
    }

    @Override
    @Transactional
    public void cancelRequest(Integer requestId) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"DRAFT".equals(request.getStatus()) && !"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException("Khong the huy yeu cau da xu ly xong.");
        }
        request.setStatus("CANCELLED");
        purchaseRequestRepository.save(request);
    }
}
