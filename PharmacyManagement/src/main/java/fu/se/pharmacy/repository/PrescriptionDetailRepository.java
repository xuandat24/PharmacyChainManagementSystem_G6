package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.PrescriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Integer> {
    List<PrescriptionDetail> findByPrescriptionId(Integer prescriptionId);
    Optional<PrescriptionDetail> findByPrescriptionIdAndMedicineId(Integer prescriptionId, Integer medicineId);
}