package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
    Optional<Medicine> findByBarcode(String barcode);
    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);
}
