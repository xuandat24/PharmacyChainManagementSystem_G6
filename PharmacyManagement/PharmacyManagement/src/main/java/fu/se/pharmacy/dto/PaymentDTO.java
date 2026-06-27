package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO cho thanh toán.
 * Dùng làm cả request (form tiền mặt) lẫn response (hiển thị kết quả).
 */
@Data
public class PaymentDTO {

    private Integer paymentId;

    @NotNull(message = "Mã hóa đơn không được để trống")
    private Integer saleId;

    private String paymentCode;

    private String paymentMethod; // CASH, ONLINE

    private String status; // PENDING, PAID, FAILED, CANCELLED, REFUNDED

    private Integer amount;

    // === Input khi thanh toán tiền mặt ===
    @Min(value = 0, message = "Tiền khách đưa không được âm")
    private Integer customerPaidAmount;

    // === Output sau khi thanh toán ===
    private Integer changeAmount;

    /** Thông tin QR / mã tham chiếu cho online payment */
    private String qrNote;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}