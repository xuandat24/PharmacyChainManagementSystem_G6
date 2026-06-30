package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.StockTransferDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferDetailRepository extends JpaRepository<StockTransferDetail, Integer> {
    List<StockTransferDetail> findByStockTransfer_StockTransferId(Integer stockTransferId);
}
