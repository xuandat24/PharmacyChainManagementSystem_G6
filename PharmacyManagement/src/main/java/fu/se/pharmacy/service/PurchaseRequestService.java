package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.PurchaseRequest;
import fu.se.pharmacy.entity.PurchaseRequestDetail;
import java.util.List;

public interface PurchaseRequestService {
    List<PurchaseRequest> getAllRequests();
    List<PurchaseRequest> getRequestsByBranch(Integer branchId);
    PurchaseRequest getRequestById(Integer id);
    PurchaseRequest saveRequest(PurchaseRequest request);
    void submitRequest(Integer requestId);
    void approveRequest(Integer requestId, Integer adminId, String adminNotes, List<PurchaseRequestDetail> approvedDetails);
    void rejectRequest(Integer requestId, Integer adminId, String adminNotes);
    void cancelRequest(Integer requestId);
}
