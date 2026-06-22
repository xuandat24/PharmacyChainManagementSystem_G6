package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PurchaseRequest")
@Data
public class PurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByID", nullable = false)
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierID", nullable = false)
    private Supplier supplier;

    @Column(name = "RequestDate", nullable = false)
    private LocalDateTime requestDate = LocalDateTime.now();

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, SUBMITTED, APPROVED, PARTIALLY_APPROVED, REJECTED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedByID")
    private Employee approvedBy;

    @Column(name = "ApprovalDate")
    private LocalDateTime approvalDate;

    @Column(name = "AdminNotes", length = 500)
    private String adminNotes;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestDetail> details = new ArrayList<>();
}
