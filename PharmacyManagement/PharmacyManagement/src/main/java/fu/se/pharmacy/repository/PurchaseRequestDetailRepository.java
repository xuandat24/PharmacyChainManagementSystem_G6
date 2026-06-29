package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PurchaseRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestDetailRepository extends JpaRepository<PurchaseRequestDetail, Integer> {
    List<PurchaseRequestDetail> findByPurchaseRequestId(Integer purchaseRequestId);
}
