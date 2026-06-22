package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.repository.MedicineRepository;
import fu.se.pharmacy.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    @Override
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    @Override
    public Medicine saveMedicine(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    @Override
    public Medicine getMedicineById(Integer id) {
        return medicineRepository.findById(id).orElse(null);
    }


    @Override
    public void deleteMedicine(Integer id) {
        Medicine medicine = getMedicineById(id);
        if (medicine != null) {
            medicine.setStatus("INACTIVE"); // Tuân thủ rule: Không xóa cứng khỏi Database
            medicineRepository.save(medicine);
        }
    }
}