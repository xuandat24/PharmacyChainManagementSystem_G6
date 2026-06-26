package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.PrescriptionDetailDTO;
import fu.se.pharmacy.dto.PrescriptionDTO;
import fu.se.pharmacy.entity.Prescription;

import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các nghiệp vụ quản lý đơn thuốc.
 */
public interface PrescriptionService {

    /** Lấy tất cả đơn thuốc của một khách hàng */
    List<PrescriptionDTO> findByCustomerId(Integer customerId);

    /** Tìm đơn thuốc theo ID */
    Optional<PrescriptionDTO> findById(Integer prescriptionId);

    /** Tìm entity gốc (dùng nội bộ) */
    Optional<Prescription> findEntityById(Integer prescriptionId);

    /** Tạo đơn thuốc mới */
    PrescriptionDTO save(PrescriptionDTO dto, Integer createdByUserId);

    /** Thêm một dòng thuốc vào đơn */
    PrescriptionDetailDTO addDetail(PrescriptionDetailDTO dto);

    /** Lấy danh sách thuốc trong đơn */
    List<PrescriptionDetailDTO> findDetails(Integer prescriptionId);

    /**
     * Kiểm tra thuốc có trong đơn còn hiệu lực và đủ số lượng không.
     * Dùng trong luồng kiểm tra trước khi thêm vào hóa đơn.
     */
    boolean isValidForSale(Integer prescriptionId, Integer medicineId, Integer requestedQty);
}