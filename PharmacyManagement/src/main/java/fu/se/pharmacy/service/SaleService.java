package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.SaleDTO;

import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các nghiệp vụ bán hàng.
 */
public interface SaleService {

    /** Lấy hoặc tạo hóa đơn DRAFT cho pharmacist */
    SaleDTO getOrCreateDraft(Integer pharmacistId, Integer branchId);

    /** Tìm hóa đơn theo ID */
    Optional<SaleDTO> findById(Integer saleId);

    /** Lấy toàn bộ hóa đơn (sắp xếp mới nhất trước) */
    List<SaleDTO> findAll();

    /** Lấy lịch sử hóa đơn của một khách hàng */
    List<SaleDTO> findByCustomerId(Integer customerId);

    /** Lấy hóa đơn theo chi nhánh */
    List<SaleDTO> findByBranchId(Integer branchId);

    /**
     * Thêm thuốc vào hóa đơn DRAFT.
     * @return null nếu thành công, chuỗi thông báo lỗi nếu thất bại
     */
    String addItem(Integer saleId, Integer medicineId, Integer quantity);

    /** Cập nhật số lượng một dòng (quantity <= 0 sẽ xóa dòng đó) */
    void updateItemQuantity(Integer saleDetailId, Integer quantity);

    /** Xóa một dòng thuốc khỏi hóa đơn */
    void removeItem(Integer saleDetailId);

    /** Gán khách hàng cho hóa đơn */
    void setCustomer(Integer saleId, Integer customerId);

    /** Gán đơn thuốc cho hóa đơn */
    void setPrescription(Integer saleId, Integer prescriptionId);

    /** Hủy hóa đơn DRAFT */
    void cancelDraft(Integer saleId);

    /**
     * Hoàn thành hóa đơn sau khi thanh toán thành công.
     * Cập nhật status → COMPLETED và trừ tồn kho FIFO.
     * Chỉ PaymentService được gọi method này.
     */
    void completeSale(Integer saleId);
}