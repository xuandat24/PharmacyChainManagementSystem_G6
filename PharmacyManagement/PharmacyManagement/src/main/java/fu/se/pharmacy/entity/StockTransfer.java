package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers")
@Data
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

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "total_value_amount", nullable = false)
    private Integer totalValueAmount = 0;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @OneToMany(mappedBy = "stockTransferId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockTransferDetail> details = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_branch_id", insertable = false, updatable = false)
    private Branch fromBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_branch_id", insertable = false, updatable = false)
    private Branch toBranch;

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) this.requestedAt = LocalDateTime.now();
    }
}
