package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.Medicine;
import java.util.List;

public interface MedicineService {
    List<Medicine> getAllMedicines();
    Medicine saveMedicine(Medicine medicine);
    Medicine getMedicineById(Integer id);
    
    void deleteMedicine(Integer id);
}