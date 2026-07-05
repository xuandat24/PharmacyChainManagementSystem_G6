package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Integer> {
    List<Sale> findByPharmacistIdAndStatus(Integer pharmacistId, String status);
    List<Sale> findByCustomerId(Integer customerId);
    List<Sale> findByBranchIdOrderBySaleDateDesc(Integer branchId);
    List<Sale> findAllByOrderBySaleDateDesc();
    List<Sale> findByStatus(String status);
    long countBySaleDateBetweenAndStatus(LocalDateTime from, LocalDateTime to, String status);
}
