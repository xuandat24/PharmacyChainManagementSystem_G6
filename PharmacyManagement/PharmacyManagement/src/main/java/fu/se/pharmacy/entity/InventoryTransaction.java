package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
@Data
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_transaction_id")
    private Integer transactionId;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "inventory_batch_id")
    private Integer inventoryBatchId;

    @Column(name = "transaction_type", nullable = false, length = 40)
    private String transactionType;

    @Column(name = "quantity", nullable = false)
    private Integer quantityChange;

    @Column(name = "unit_cost")
    private Integer unitCost;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at")
    private LocalDateTime transactionDate;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;

    // Navigation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", insertable = false, updatable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", insertable = false, updatable = false)
    private Medicine medicine;

    @PrePersist
    protected void onCreate() {
        if (this.transactionDate == null) this.transactionDate = LocalDateTime.now();
    }
}
