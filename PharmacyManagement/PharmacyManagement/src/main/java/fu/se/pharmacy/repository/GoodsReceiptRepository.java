package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {
    List<GoodsReceipt> findByBranch_BranchId(Integer branchId);
    List<GoodsReceipt> findByStatus(String status);
    List<GoodsReceipt> findByBranch_BranchIdAndStatus(Integer branchId, String status);
}
