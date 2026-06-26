package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetail, Integer> {
    List<SaleDetail> findBySaleId(Integer saleId);
    Optional<SaleDetail> findBySaleIdAndMedicineId(Integer saleId, Integer medicineId);
}