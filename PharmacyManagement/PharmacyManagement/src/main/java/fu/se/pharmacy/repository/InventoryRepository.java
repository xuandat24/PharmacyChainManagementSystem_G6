package fu.se.pharmacy.repository;

import fu.se.pharmacy.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    Optional<Inventory> findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(Integer branchId, Integer medicineId, String batchNumber);

    List<Inventory> findByBranch_BranchId(Integer branchId);

    List<Inventory> findByBranch_BranchIdAndMedicine_MedicineId(Integer branchId, Integer medicineId);

    // Lấy danh sách tồn kho của một chi nhánh mà lượng tồn > 0 và chưa hết hạn (dành cho Dược sĩ)
    @Query("SELECT i FROM Inventory i WHERE i.branch.branchId = :branchId AND i.quantity > 0 AND i.expiryDate > :today")
    List<Inventory> findAvailableStock(@Param("branchId") Integer branchId, @Param("today") LocalDate today);

    // Tìm kiếm theo tên thuốc hoặc barcode tại một chi nhánh
    @Query("SELECT i FROM Inventory i WHERE i.branch.branchId = :branchId AND " +
           "(LOWER(i.medicine.medicineName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "i.medicine.barcode = :query OR i.batchNumber = :query)")
    List<Inventory> searchStock(@Param("branchId") Integer branchId, @Param("query") String query);

    // Tìm kiếm trên toàn hệ thống
    @Query("SELECT i FROM Inventory i WHERE LOWER(i.medicine.medicineName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "i.medicine.barcode = :query OR i.batchNumber = :query")
    List<Inventory> searchStockSystemWide(@Param("query") String query);

    // Lọc thuốc sắp hết hạn (ví dụ còn dưới n ngày và lớn hơn ngày hiện tại)
    @Query("SELECT i FROM Inventory i WHERE i.branch.branchId = :branchId AND i.expiryDate BETWEEN :today AND :warningDate AND i.quantity > 0")
    List<Inventory> findExpiringSoon(@Param("branchId") Integer branchId, @Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    // Lọc thuốc sắp hết hạn trên toàn hệ thống
    @Query("SELECT i FROM Inventory i WHERE i.expiryDate BETWEEN :today AND :warningDate AND i.quantity > 0")
    List<Inventory> findExpiringSoonSystemWide(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    // Lọc thuốc đã hết hạn
    @Query("SELECT i FROM Inventory i WHERE i.branch.branchId = :branchId AND i.expiryDate <= :today AND i.quantity > 0")
    List<Inventory> findExpired(@Param("branchId") Integer branchId, @Param("today") LocalDate today);

    // Lọc thuốc đã hết hạn trên toàn hệ thống
    @Query("SELECT i FROM Inventory i WHERE i.expiryDate <= :today AND i.quantity > 0")
    List<Inventory> findExpiredSystemWide(@Param("today") LocalDate today);
}
