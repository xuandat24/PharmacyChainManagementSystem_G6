package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Integer> {
    List<InventoryTransaction> findByBranch_BranchId(Integer branchId);
    List<InventoryTransaction> findByBranch_BranchIdAndMedicine_MedicineId(Integer branchId, Integer medicineId);
}
