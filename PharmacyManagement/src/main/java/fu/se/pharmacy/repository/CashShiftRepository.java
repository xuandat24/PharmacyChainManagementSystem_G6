package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.CashShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashShiftRepository extends JpaRepository<CashShift, Integer> {
    Optional<CashShift> findByPharmacistIdAndStatus(Integer pharmacistId, String status);
    List<CashShift> findByBranchIdOrderByOpenedAtDesc(Integer branchId);
    List<CashShift> findByStatus(String status);
}