package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_counts")
@Data
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_count_id")
    private Integer stockCountId;

    @Column(name = "count_code", nullable = false, unique = true, length = 40)
    private String countCode;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    // FIX: approvedBy là Integer FK (StockCountServiceImpl dùng setApprovedBy(adminId))
    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "count_date")
    private LocalDateTime countDate;

    // FIX: approvedAt thay vì approvalDate (StockCountServiceImpl gọi setApprovedAt)
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "total_variance_amount", nullable = false)
    private Integer totalVarianceAmount = 0;

    // FIX: note thay vì notes (StockCountServiceImpl gọi setNote)
    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @OneToMany(mappedBy = "stockCountId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockCountDetail> details = new ArrayList<>();

    // Navigation lazy (optional, không bắt buộc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @PrePersist
    protected void onCreate() {
        if (this.countDate == null) this.countDate = LocalDateTime.now();
        if (this.countCode == null) this.countCode = "SC-" + System.currentTimeMillis();
    }
}