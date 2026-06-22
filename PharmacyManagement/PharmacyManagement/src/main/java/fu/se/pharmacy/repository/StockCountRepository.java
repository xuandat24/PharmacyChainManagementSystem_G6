package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Integer> {
    List<StockCount> findByBranch_BranchId(Integer branchId);
    List<StockCount> findByStatus(String status);
    List<StockCount> findByBranch_BranchIdAndStatus(Integer branchId, String status);
}
