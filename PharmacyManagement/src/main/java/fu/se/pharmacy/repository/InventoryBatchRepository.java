package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Integer> {

    /**
     * Tổng số lượng khả dụng của một thuốc tại một chi nhánh.
     * Người 3 dùng để kiểm tra tồn trước khi thêm vào hóa đơn.
     */
    @Query("SELECT COALESCE(SUM(b.quantityOnHand), 0) " +
            "FROM InventoryBatch b " +
            "WHERE b.branchId = :branchId " +
            "AND b.medicineId = :medicineId " +
            "AND b.status = 'AVAILABLE'")
    Integer sumStock(@Param("branchId") Integer branchId,
                     @Param("medicineId") Integer medicineId);

    /**
     * Lấy danh sách lô theo thứ tự hạn dùng gần nhất trước (FIFO xuất trước hạn).
     * Dùng trong deductInventoryFifo.
     */
    List<InventoryBatch> findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(
            Integer branchId, Integer medicineId, String status);
}