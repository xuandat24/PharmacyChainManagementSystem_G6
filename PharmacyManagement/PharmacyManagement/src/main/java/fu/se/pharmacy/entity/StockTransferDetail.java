package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "StockTransferDetail")
@Data
public class StockTransferDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransferDetailID")
    private Integer transferDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TransferID", nullable = false)
    private StockTransfer stockTransfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "BatchNumber", length = 50)
    private String batchNumber;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;
}
