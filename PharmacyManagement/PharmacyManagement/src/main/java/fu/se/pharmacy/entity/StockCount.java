package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "StockCount")
@Data
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StockCountID")
    private Integer stockCountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByID", nullable = false)
    private Employee createdBy;

    @Column(name = "CountDate", nullable = false)
    private LocalDateTime countDate = LocalDateTime.now();

    @Column(name = "Status", nullable = false, length = 25)
    private String status = "DRAFT"; // DRAFT, PENDING_ADMIN_APPROVAL, COMPLETED, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApprovedByID")
    private Employee approvedBy;

    @Column(name = "ApprovalDate")
    private LocalDateTime approvalDate;

    @Column(name = "Notes", length = 500)
    private String notes;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCountDetail> details = new ArrayList<>();
}
