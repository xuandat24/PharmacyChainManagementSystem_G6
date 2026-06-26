package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sale_details")
@Data
public class SaleDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_detail_id")
    private Integer saleDetailId;

    @Column(name = "sale_id", nullable = false)
    private Integer saleId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "inventory_batch_id")
    private Integer inventoryBatchId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "line_amount", nullable = false)
    private Integer lineAmount; // quantity * unit_price

    @Transient
    public Integer getSubtotal() {
        if (unitPrice == null || quantity == null) return 0;
        return unitPrice * quantity;
    }
}