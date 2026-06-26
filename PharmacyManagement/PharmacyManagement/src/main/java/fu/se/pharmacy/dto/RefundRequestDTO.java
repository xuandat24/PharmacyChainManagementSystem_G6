package fu.se.pharmacy.dto;

import java.time.LocalDateTime;

public class RefundRequestDTO {
    private Integer refundRequestId;
    private Integer saleId;
    private String saleCode;
    private Integer requestedBy;
    private String requestedByName;   // join từ app_users
    private Integer approvedBy;
    private String approvedByName;    // join từ app_users
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private Integer refundAmount;
    private String reason;
    private String status;

    public Integer getRefundRequestId() {
        return refundRequestId;
    }

    public void setRefundRequestId(Integer refundRequestId) {
        this.refundRequestId = refundRequestId;
    }

    public Integer getSaleId() {
        return saleId;
    }

    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }

    public String getSaleCode() {
        return saleCode;
    }

    public void setSaleCode(String saleCode) {
        this.saleCode = saleCode;
    }

    public Integer getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Integer requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Integer getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Integer refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public RefundRequestDTO() {
    }

    @Override
    public String toString() {
        return "RefundRequestDTO{" +
                "refundRequestId=" + refundRequestId +
                ", saleId=" + saleId +
                ", saleCode='" + saleCode + '\'' +
                ", requestedBy=" + requestedBy +
                ", requestedByName='" + requestedByName + '\'' +
                ", approvedBy=" + approvedBy +
                ", approvedByName='" + approvedByName + '\'' +
                ", requestedAt=" + requestedAt +
                ", approvedAt=" + approvedAt +
                ", refundAmount=" + refundAmount +
                ", reason='" + reason + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
