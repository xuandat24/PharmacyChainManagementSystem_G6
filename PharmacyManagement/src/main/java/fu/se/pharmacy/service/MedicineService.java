package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.Medicine;

import java.util.List;

public interface MedicineService {
    List<Medicine> getAllMedicines();
    // FIX: thêm performedByUserId để ghi Audit Log khi giá bán (salePrice) thay đổi.
    Medicine saveMedicine(Medicine medicine, Integer performedByUserId);
    Medicine getMedicineById(Integer id);

    void deleteMedicine(Integer id);
}