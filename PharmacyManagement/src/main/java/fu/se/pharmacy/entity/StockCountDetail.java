package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stock_count_details")
@Data
public class StockCountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_count_detail_id")
    private Integer stockCountDetailId;

    @Column(name = "stock_count_id", nullable = false)
    private Integer stockCountId;

    @Column(name = "inventory_batch_id", nullable = false)
    private Integer inventoryBatchId;

    @Column(name = "system_quantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "actual_quantity", nullable = false)
    private Integer actualQuantity;

    // variance_quantity = actual - system (computed column in DB)
    @Column(name = "reason", columnDefinition = "NVARCHAR(255)")
    private String reason;

    // Getter helper
    public int getDifference() {
        return (actualQuantity == null ? 0 : actualQuantity)
             - (systemQuantity == null ? 0 : systemQuantity);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_batch_id", insertable = false, updatable = false)
    private InventoryBatch inventoryBatch;
}
