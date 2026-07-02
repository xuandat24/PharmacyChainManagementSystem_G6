package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.repository.MedicineRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    // FIX: bổ sung ghi Audit Log khi đổi giá thuốc — nằm trong danh sách hành động
    // bắt buộc ghi log của flow (mục VI.8) nhưng trước đây chưa được gọi.
    private final AuditLogService auditLogService;

    @Override
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    @Override
    public Medicine saveMedicine(Medicine medicine, Integer performedByUserId) {
        Integer oldPrice = null;
        boolean isUpdate = medicine.getMedicineId() != null;
        if (isUpdate) {
            oldPrice = medicineRepository.findById(medicine.getMedicineId())
                    .map(Medicine::getSalePrice).orElse(null);
        }

        Medicine saved = medicineRepository.save(medicine);

        boolean priceChanged = isUpdate && oldPrice != null
                && !oldPrice.equals(saved.getSalePrice());
        if (priceChanged) {
            auditLogService.log(performedByUserId, null, "MEDICINE_PRICE_CHANGE", "Medicine",
                    saved.getMedicineId(), String.valueOf(oldPrice), String.valueOf(saved.getSalePrice()),
                    "Cập nhật giá bán thuốc " + saved.getMedicineName());
        }

        return saved;
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