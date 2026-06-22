package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Integer> {
    List<PurchaseRequest> findByBranch_BranchId(Integer branchId);
    List<PurchaseRequest> findByStatus(String status);
    List<PurchaseRequest> findByBranch_BranchIdAndStatus(Integer branchId, String status);
}
