package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "stock_transfer_details")
@Data
public class StockTransferDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_transfer_detail_id")
    private Integer stockTransferDetailId;

    @Column(name = "stock_transfer_id", nullable = false)
    private Integer stockTransferId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "from_inventory_batch_id")
    private Integer fromInventoryBatchId;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "sent_quantity")
    private Integer sentQuantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", insertable = false, updatable = false)
    private Medicine medicine;
}
