package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {
    // FIX: GoodsReceiptServiceImpl dùng findByBranchId (entity có branchId integer FK)
    List<GoodsReceipt> findByBranchId(Integer branchId);
    List<GoodsReceipt> findByBranchIdOrderByReceivedAtDesc(Integer branchId);
    List<GoodsReceipt> findByStatus(String status);
}