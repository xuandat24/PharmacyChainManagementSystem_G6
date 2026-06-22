package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Employee;
import fu.se.pharmacy.entity.PurchaseRequest;
import fu.se.pharmacy.entity.PurchaseRequestDetail;
import fu.se.pharmacy.repository.EmployeeRepository;
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

    @Autowired
    private PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    private PurchaseRequestDetailRepository detailRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public List<PurchaseRequest> getAllRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    public List<PurchaseRequest> getRequestsByBranch(Integer branchId) {
        return purchaseRequestRepository.findByBranch_BranchId(branchId);
    }

    @Override
    public PurchaseRequest getRequestById(Integer id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu nhập hàng ID: " + id));
    }

    @Override
    @Transactional
    public PurchaseRequest saveRequest(PurchaseRequest request) {
        // Đảm bảo quan hệ hai chiều được thiết lập đúng
        if (request.getDetails() != null) {
            for (PurchaseRequestDetail detail : request.getDetails()) {
                detail.setPurchaseRequest(request);
            }
        }
        return purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void submitRequest(Integer requestId) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"DRAFT".equals(request.getStatus())) {
            throw new IllegalStateException("Chỉ yêu cầu dạng Nháp (DRAFT) mới được phép gửi duyệt.");
        }
        request.setStatus("SUBMITTED");
        purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void approveRequest(Integer requestId, Integer adminId, String adminNotes, List<PurchaseRequestDetail> approvedDetails) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException("Chỉ yêu cầu đang Chờ duyệt (SUBMITTED) mới được phép duyệt.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên Admin ID: " + adminId));

        // Bản đồ hóa các detail gửi lên bằng ID để đối chiếu
        Map<Integer, Integer> approvedQtyMap = approvedDetails.stream()
                .filter(d -> d.getRequestDetailId() != null)
                .collect(Collectors.toMap(PurchaseRequestDetail::getRequestDetailId, PurchaseRequestDetail::getQuantityApproved));

        boolean fullyApproved = true;
        boolean hasSomeApproval = false;

        for (PurchaseRequestDetail detail : request.getDetails()) {
            Integer approvedQty = approvedQtyMap.get(detail.getRequestDetailId());
            if (approvedQty == null) {
                approvedQty = 0; // Từ chối mặt hàng này
            }

            if (approvedQty < 0 || approvedQty > detail.getQuantityRequested()) {
                throw new IllegalArgumentException("Số lượng duyệt không hợp lệ cho thuốc: " + detail.getMedicine().getMedicineName());
            }

            detail.setQuantityApproved(approvedQty);
            detailRepository.save(detail);

            if (approvedQty < detail.getQuantityRequested()) {
                fullyApproved = false;
            }
            if (approvedQty > 0) {
                hasSomeApproval = true;
            }
        }

        request.setApprovedBy(admin);
        request.setApprovalDate(LocalDateTime.now());
        request.setAdminNotes(adminNotes);

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
            throw new IllegalStateException("Chỉ yêu cầu đang Chờ duyệt (SUBMITTED) mới được phép từ chối.");
        }

        Employee admin = employeeRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên Admin ID: " + adminId));

        request.setApprovedBy(admin);
        request.setApprovalDate(LocalDateTime.now());
        request.setAdminNotes(adminNotes);
        request.setStatus("REJECTED");

        for (PurchaseRequestDetail detail : request.getDetails()) {
            detail.setQuantityApproved(0);
            detailRepository.save(detail);
        }

        purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void cancelRequest(Integer requestId) {
        PurchaseRequest request = getRequestById(requestId);
        if (!"DRAFT".equals(request.getStatus()) && !"SUBMITTED".equals(request.getStatus())) {
            throw new IllegalStateException("Không thể hủy yêu cầu đã được xử lý xong.");
        }
        request.setStatus("CANCELLED");
        purchaseRequestRepository.save(request);
    }
}
