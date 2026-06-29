package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Integer> {
    List<StockTransfer> findByFromBranchIdOrToBranchId(Integer fromBranchId, Integer toBranchId);
    List<StockTransfer> findByStatus(String status);
}
