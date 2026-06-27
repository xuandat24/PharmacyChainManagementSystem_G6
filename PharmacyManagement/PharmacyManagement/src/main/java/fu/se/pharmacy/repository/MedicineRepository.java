package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
    List<Medicine> findByStatus(String status);
    Optional<Medicine> findByBarcode(String barcode);
    List<Medicine> findByMedicineNameContainingIgnoreCaseAndStatus(String name, String status);
}