package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.PurchaseRequest;
import fu.se.pharmacy.entity.PurchaseRequestDetail;
import fu.se.pharmacy.repository.PurchaseRequestDetailRepository;
import fu.se.pharmacy.repository.PurchaseRequestRepository;
import fu.se.pharmacy.service.PurchaseRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    @Autowired private PurchaseRequestRepository purchaseRequestRepository;
    @Autowired private PurchaseRequestDetailRepository detailRepository;

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

        // Tính tổng dự kiến
        int total = 0;
        if (request.getDetails() != null) {
            for (PurchaseRequestDetail d : request.getDetails()) {
                total += (d.getExpectedUnitPrice() != null ? d.getExpectedUnitPrice() : 0)
                        * (d.getRequestedQuantity() != null ? d.getRequestedQuantity() : 0);
            }
        }
        request.setTotalEstimatedAmount(total);

        // FIX: Bước 1 - lưu PurchaseRequest TRƯỚC, KHÔNG kèm details
        // (entity có cascade=ALL trên details, nếu save cả request lẫn details cùng lúc
        //  thì Hibernate cascade insert detail TRƯỚC KHI vòng lặp set purchaseRequestId
        //  chạy tới → detail bị insert với purchase_request_id = NULL → lỗi NOT NULL constraint)
        List<PurchaseRequestDetail> details = request.getDetails();
        request.setDetails(null); // tránh cascade insert detail ở bước này

        PurchaseRequest saved = purchaseRequestRepository.save(request);

        // FIX: Bước 2 - giờ đã có purchaseRequestId, gán vào từng detail rồi mới save
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
        request.setStatus("SUBMITTED");
        purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void approveRequest(Integer requestId, Integer adminId, String adminNotes,
                               List<PurchaseRequestDetail> approvedDetails) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException("Chi yeu cau SUBMITTED moi duoc duyet.");
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

        if (hasSomeApproval) {
            request.setStatus(fullyApproved ? "APPROVED" : "PARTIALLY_APPROVED");
        } else {
            request.setStatus("REJECTED");
        }

        purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void rejectRequest(Integer requestId, Integer adminId, String adminNotes) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException("Chi yeu cau SUBMITTED moi duoc tu choi.");
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