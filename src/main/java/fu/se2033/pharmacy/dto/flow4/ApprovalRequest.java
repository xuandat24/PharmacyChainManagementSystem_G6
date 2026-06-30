package fu.se2033.pharmacy.dto.flow4;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApprovalRequest {
    @NotNull(message = "Người duyệt không được để trống")
    private Integer approvedBy;

    @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
    private String reason;

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
