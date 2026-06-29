package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts")
@Data
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goods_receipt_id")
    private Integer receiptId;

    // FIX: receipt_code là NOT NULL UNIQUE - phải tự sinh nếu không set
    @Column(name = "receipt_code", nullable = false, unique = true, length = 40)
    private String receiptCode;

    @Column(name = "purchase_request_id", nullable = false)
    private Integer purchaseRequestId;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @Column(name = "received_by", nullable = false)
    private Integer receivedBy;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "total_actual_amount", nullable = false)
    private Integer totalActualAmount = 0;

    @Column(name = "has_variance", nullable = false)
    private Boolean hasVariance = false;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @OneToMany(mappedBy = "goodsReceiptId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptDetail> details = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @PrePersist
    protected void onCreate() {
        // FIX: tự sinh receiptCode khi chưa có — tránh NOT NULL constraint fail
        if (this.receiptCode == null || this.receiptCode.isBlank()) {
            this.receiptCode = "GR-" + System.currentTimeMillis();
        }
        if (this.receivedAt == null) {
            this.receivedAt = LocalDateTime.now();
        }
    }
}