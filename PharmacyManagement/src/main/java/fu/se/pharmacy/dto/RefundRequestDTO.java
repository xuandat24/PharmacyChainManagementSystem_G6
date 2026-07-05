package fu.se.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Vui lòng nhập lý do hoàn tiền")
    @Size(min = 10, max = 500, message = "Lý do hoàn tiền phải từ 10 đến 500 ký tự")
    private String reason;

    // Output fields
    private String  saleCode;
    private Integer requestedBy;
    private String  requestedByName;
    private Integer approvedBy;
    private String  approvedByName;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private Integer refundAmount;
    private String  status;
    private Integer saleTotal;
    private String  paymentMethod;
}