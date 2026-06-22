package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.StockCountDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockCountDetailRepository extends JpaRepository<StockCountDetail, Integer> {
    List<StockCountDetail> findByStockCount_StockCountId(Integer stockCountId);
}
