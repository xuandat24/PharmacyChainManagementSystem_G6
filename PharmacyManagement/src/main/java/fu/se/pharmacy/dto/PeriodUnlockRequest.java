package fu.se.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PeriodUnlockRequest {
    @NotNull(message = "Người mở khóa kỳ không được để trống")
    private Integer unlockedBy;

    @NotBlank(message = "Lý do mở khóa không được để trống")
    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    public Integer getUnlockedBy() { return unlockedBy; }
    public void setUnlockedBy(Integer unlockedBy) { this.unlockedBy = unlockedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
