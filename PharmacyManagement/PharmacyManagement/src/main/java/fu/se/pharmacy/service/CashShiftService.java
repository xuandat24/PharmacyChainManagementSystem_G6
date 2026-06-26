package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.CashShiftDTO;


import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các nghiệp vụ quản lý ca bán hàng.
 */
public interface CashShiftService {

    /** Mở ca mới cho pharmacist. Nếu đã có ca OPEN thì trả về ca đó. */
    CashShiftDTO openShift(Integer pharmacistId, Integer branchId);

    /**
     * Chốt ca: pharmacist nhập tiền thực tế.
     * Nếu chênh lệch vượt ngưỡng → status = PENDING_ADMIN_REVIEW.
     */
    CashShiftDTO closeShift(Integer shiftId, CashShiftDTO dto);

    /** Branch Manager xác nhận ca đã chốt */
    CashShiftDTO confirmShift(Integer shiftId, Integer managerId);

    /** Lấy ca đang OPEN của pharmacist */
    Optional<CashShiftDTO> getOpenShift(Integer pharmacistId);

    /** Lấy toàn bộ ca của một chi nhánh */
    List<CashShiftDTO> findByBranchId(Integer branchId);

    /** Lấy các ca đang chờ Admin xem xét */
    List<CashShiftDTO> findPendingAdminReview();

    /** Tìm ca theo ID */
    Optional<CashShiftDTO> findById(Integer shiftId);
}