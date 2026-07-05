package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO cho thanh toán.
 */
@Data
public class PaymentDTO {

    private Integer paymentId;

    @NotNull(message = "Mã hóa đơn không được để trống")
    private Integer saleId;

    @NotNull(message = "Vui lòng nhập số tiền khách đưa")
    @Min(value = 1000, message = "Số tiền khách đưa tối thiểu 1,000 đồng")
    private Integer customerPaidAmount;

    // Output fields
    private String  paymentCode;
    private String  paymentMethod;
    private String  status;
    private Integer amount;
    private Integer changeAmount;
    private String  qrNote;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}