package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Integer> {
    // FIX: PurchaseRequestServiceImpl gọi findByBranchIdOrderByRequestDateDesc
    List<PurchaseRequest> findByBranchIdOrderByRequestDateDesc(Integer branchId);
    List<PurchaseRequest> findByStatus(String status);
}