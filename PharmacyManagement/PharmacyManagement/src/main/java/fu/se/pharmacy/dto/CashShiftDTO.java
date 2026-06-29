package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO dùng khi pharmacist chốt ca (nhập tiền thực tế).
 */
@Data
public class CashShiftDTO {

    private Integer cashShiftId;

    @NotNull(message = "Tiền thực tế không được để trống")
    @Min(value = 0, message = "Tiền thực tế không được âm")
    private Integer actualCashAmount;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    // Output fields
    private Integer branchId;
    private Integer pharmacistId;
    private String  pharmacistName;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Integer openingCashAmount;
    private Integer systemCashAmount;
    private Integer differenceAmount;
    private String  status;
    private Integer managerConfirmedBy;
    private String  managerName;
    private LocalDateTime managerConfirmedAt;
}