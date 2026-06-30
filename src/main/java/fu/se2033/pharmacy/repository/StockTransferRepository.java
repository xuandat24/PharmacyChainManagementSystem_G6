package fu.se2033.pharmacy.repository;

import fu.se2033.pharmacy.common.enums.StockTransferStatus;
import fu.se2033.pharmacy.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Integer> {
    Optional<StockTransfer> findByTransferCode(String transferCode);
    List<StockTransfer> findByFromBranchIdOrToBranchIdOrderByRequestedAtDesc(Integer fromBranchId, Integer toBranchId);
    List<StockTransfer> findByStatusOrderByRequestedAtDesc(StockTransferStatus status);
    int countByStatus(StockTransferStatus status);
}
