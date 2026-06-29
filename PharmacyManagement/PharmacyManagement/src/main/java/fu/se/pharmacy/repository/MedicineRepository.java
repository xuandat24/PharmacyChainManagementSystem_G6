package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    // Từ P3 (dùng trong SaleService, InventoryBatch)
    List<Medicine> findByStatus(String status);
    Optional<Medicine> findByMedicineCode(String medicineCode);

    // Từ P2 (dùng trong GoodsReceiptServiceImpl)
    Optional<Medicine> findByBarcode(String barcode);
    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);
}
