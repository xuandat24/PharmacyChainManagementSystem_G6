package fu.se.pharmacy.service;

import fu.se.pharmacy.dto.PaymentDTO;
import java.util.Optional;

/**
 * Interface định nghĩa các nghiệp vụ thanh toán.
 */
public interface PaymentService {

    /** Thanh toán tiền mặt */
    PaymentDTO processCash(PaymentDTO dto);

    /** Tạo thanh toán online, trả về thông tin QR */
    PaymentDTO createOnlinePayment(Integer saleId);

    /**
     * Nhận callback từ payment gateway → xác nhận PAID.
     * Chỉ callback hợp lệ mới được phép gọi.
     */
    void handleCallback(Integer paymentId, String gatewayCode, Integer amount, String rawMsg);

    /** Tìm payment theo sale_id */
    Optional<PaymentDTO> findBySaleId(Integer saleId);
}