package fu.se.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO cho yêu cầu hoàn tiền.
 */
@Data
public class RefundRequestDTO {

    private Integer refundRequestId;

    @NotNull(message = "Mã hóa đơn không được để trống")
    private Integer saleId;

    private String saleCode;          // join từ sales
    private Integer requestedBy;
    private String requestedByName;   // join từ app_users
    private Integer approvedBy;
    private String approvedByName;    // join từ app_users
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private Integer refundAmount;

    @NotBlank(message = "Vui lòng nhập lý do hoàn tiền")
    private String reason;

    private String status; // PENDING, APPROVED, REJECTED, COMPLETED

    // Thông tin hóa đơn gốc (để hiển thị trong form)
    private Integer saleTotal;
    private String paymentMethod;     // CASH hoặc ONLINE
}