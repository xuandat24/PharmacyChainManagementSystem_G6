package fu.se.pharmacy.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import fu.se.pharmacy.common.enums.StockTransferStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
public class StockTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_transfer_id")
    private Integer stockTransferId;

    @Column(name = "transfer_code", nullable = false, unique = true, length = 40)
    private String transferCode;

    @Column(name = "from_branch_id", nullable = false)
    private Integer fromBranchId;

    @Column(name = "to_branch_id", nullable = false)
    private Integer toBranchId;

    @Column(name = "requested_by", nullable = false)
    private Integer requestedBy;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "sent_by")
    private Integer sentBy;

    @Column(name = "received_by")
    private Integer receivedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StockTransferStatus status;

    @Column(name = "total_value_amount", nullable = false)
    private Integer totalValueAmount;

    @Column(name = "note", length = 500)
    private String note;

    @JsonManagedReference
    @OneToMany(mappedBy = "stockTransfer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockTransferDetail> details = new ArrayList<>();

    public void addDetail(StockTransferDetail detail) {
        details.add(detail);
        detail.setStockTransfer(this);
    }

    public Integer getStockTransferId() { return stockTransferId; }
    public void setStockTransferId(Integer stockTransferId) { this.stockTransferId = stockTransferId; }
    public String getTransferCode() { return transferCode; }
    public void setTransferCode(String transferCode) { this.transferCode = transferCode; }
    public Integer getFromBranchId() { return fromBranchId; }
    public void setFromBranchId(Integer fromBranchId) { this.fromBranchId = fromBranchId; }
    public Integer getToBranchId() { return toBranchId; }
    public void setToBranchId(Integer toBranchId) { this.toBranchId = toBranchId; }
    public Integer getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Integer requestedBy) { this.requestedBy = requestedBy; }
    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }
    public Integer getSentBy() { return sentBy; }
    public void setSentBy(Integer sentBy) { this.sentBy = sentBy; }
    public Integer getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Integer receivedBy) { this.receivedBy = receivedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public StockTransferStatus getStatus() { return status; }
    public void setStatus(StockTransferStatus status) { this.status = status; }
    public Integer getTotalValueAmount() { return totalValueAmount; }
    public void setTotalValueAmount(Integer totalValueAmount) { this.totalValueAmount = totalValueAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<StockTransferDetail> getDetails() { return details; }
    public void setDetails(List<StockTransferDetail> details) { this.details = details; }
}
