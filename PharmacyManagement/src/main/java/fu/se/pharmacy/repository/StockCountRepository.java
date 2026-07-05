package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Integer> {
    // FIX: StockCountServiceImpl gọi findByBranchIdOrderByCountDateDesc thay vì findByBranch_BranchId
    List<StockCount> findByBranchIdOrderByCountDateDesc(Integer branchId);
    List<StockCount> findByStatus(String status);
}