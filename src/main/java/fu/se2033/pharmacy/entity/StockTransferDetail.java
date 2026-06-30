package fu.se2033.pharmacy.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "stock_transfer_details")
public class StockTransferDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_transfer_detail_id")
    private Integer stockTransferDetailId;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    private StockTransfer stockTransfer;

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

    public Integer getStockTransferDetailId() { return stockTransferDetailId; }
    public void setStockTransferDetailId(Integer stockTransferDetailId) { this.stockTransferDetailId = stockTransferDetailId; }
    public StockTransfer getStockTransfer() { return stockTransfer; }
    public void setStockTransfer(StockTransfer stockTransfer) { this.stockTransfer = stockTransfer; }
    public Integer getMedicineId() { return medicineId; }
    public void setMedicineId(Integer medicineId) { this.medicineId = medicineId; }
    public Integer getFromInventoryBatchId() { return fromInventoryBatchId; }
    public void setFromInventoryBatchId(Integer fromInventoryBatchId) { this.fromInventoryBatchId = fromInventoryBatchId; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public Integer getSentQuantity() { return sentQuantity; }
    public void setSentQuantity(Integer sentQuantity) { this.sentQuantity = sentQuantity; }
    public Integer getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
}
