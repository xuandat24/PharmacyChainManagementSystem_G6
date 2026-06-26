package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO nhận dữ liệu khi pharmacist chốt ca (nhập tiền thực tế).
 */
@Data
public class CashShiftDTO {

    @NotNull(message = "Tiền thực tế không được để trống")
    @Min(value = 0, message = "Tiền không âm")
    private Integer actualCashAmount;

    private String note;
    private Integer cashShiftId;
    private Integer branchId;
    private Integer pharmacistId;
    private String pharmacistName;       // join từ app_users
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private Integer openingCashAmount;
    private Integer systemCashAmount;
    private Integer differenceAmount;
    private String status;
    private Integer managerConfirmedBy;
    private String managerName;          // join từ app_users
    private LocalDateTime managerConfirmedAt;

}