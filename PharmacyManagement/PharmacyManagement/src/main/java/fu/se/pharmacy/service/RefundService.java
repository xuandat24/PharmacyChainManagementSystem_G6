package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.RefundRequestDTO;


import java.util.List;
import java.util.Optional;

/**
 * Interface định nghĩa các nghiệp vụ hoàn tiền.
 */
public interface RefundService {

    /** Tạo yêu cầu hoàn tiền */
    RefundRequestDTO createRequest(RefundRequestDTO dto, Integer requestedByUserId);

    /**
     * Branch Manager duyệt — chỉ trong hạn mức và không phải ONLINE.
     * Ném exception nếu vượt hạn mức.
     */
    RefundRequestDTO approveByManager(Integer refundId, Integer managerId);

    /** Admin duyệt — không giới hạn */
    RefundRequestDTO approveByAdmin(Integer refundId, Integer adminId);

    /** Từ chối yêu cầu hoàn tiền */
    RefundRequestDTO reject(Integer refundId, Integer reviewerId);

    /** Lấy toàn bộ yêu cầu (mới nhất trước) */
    List<RefundRequestDTO> findAll();

    /** Lấy các yêu cầu đang chờ duyệt */
    List<RefundRequestDTO> findPending();

    /** Tìm theo ID */
    Optional<RefundRequestDTO> findById(Integer refundId);
}