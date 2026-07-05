package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.CashShiftDTO;
import java.util.List;
import java.util.Optional;

public interface CashShiftService {
    CashShiftDTO openShift(Integer pharmacistId, Integer branchId);
    CashShiftDTO closeShift(Integer shiftId, CashShiftDTO dto);
    CashShiftDTO confirmShift(Integer shiftId, Integer managerId);
    Optional<CashShiftDTO> getOpenShift(Integer pharmacistId);
    List<CashShiftDTO> findByBranchId(Integer branchId);
    // FIX: Admin cần xem tất cả ca — thêm findAll()
    List<CashShiftDTO> findAll();
    List<CashShiftDTO> findPendingAdminReview();
    Optional<CashShiftDTO> findById(Integer shiftId);
}