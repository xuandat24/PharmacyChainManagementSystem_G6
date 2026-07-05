package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batches")
@Data
public class InventoryBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_batch_id")
    private Integer inventoryBatchId;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand = 0;

    @Column(name = "unit_cost", nullable = false)
    private Integer unitCost = 0;

    @Column(name = "status", nullable = false)
    private String status = "AVAILABLE"; // AVAILABLE, EXPIRED, DAMAGED, RECALLED, DISPOSED

    @Column(name = "received_date", updatable = false)
    private LocalDateTime receivedDate;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.receivedDate == null) this.receivedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}