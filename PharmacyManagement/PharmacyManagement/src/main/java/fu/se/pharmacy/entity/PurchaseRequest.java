package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_requests")
@Data
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_request_id")
    private Integer purchaseRequestId;

    @Column(name = "request_code", nullable = false, unique = true, length = 40)
    private String requestCode;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "supplier_id", nullable = false)
    private Integer supplierId;

    @Column(name = "requested_by", nullable = false)
    private Integer requestedBy;

    // FIX: PurchaseRequestServiceImpl gọi setApprovedBy(adminId Integer)
    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    // FIX: PurchaseRequestServiceImpl gọi setApprovedAt - DB có cột approved_at
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "total_estimated_amount", nullable = false)
    private Integer totalEstimatedAmount = 0;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "reject_reason", columnDefinition = "NVARCHAR(500)")
    private String rejectReason;

    @OneToMany(mappedBy = "purchaseRequestId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestDetail> details = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", insertable = false, updatable = false)
    private Supplier supplier;

    @PrePersist
    protected void onCreate() {
        if (this.requestDate == null) this.requestDate = LocalDateTime.now();
        if (this.requestCode == null) this.requestCode = "PR-" + System.currentTimeMillis();
    }
}