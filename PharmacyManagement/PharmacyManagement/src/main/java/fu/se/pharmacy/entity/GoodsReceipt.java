package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GoodsReceipt")
@Data
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceiptID")
    private Integer receiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PurchaseRequestID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceivedByID", nullable = false)
    private Employee receivedBy;

    @Column(name = "ReceivedDate", nullable = false)
    private LocalDateTime receivedDate = LocalDateTime.now();

    @Column(name = "Status", nullable = false, length = 25)
    private String status = "DRAFT"; // DRAFT, PENDING_ADMIN_APPROVAL, POSTED, REJECTED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedByID")
    private Employee approvedBy;

    @Column(name = "ApprovalDate")
    private LocalDateTime approvalDate;

    @Column(name = "AdminNotes", length = 500)
    private String adminNotes;

    @Column(name = "TotalAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptDetail> details = new ArrayList<>();
}
