package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "StockCountDetail")
@Data
public class StockCountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StockCountDetailID")
    private Integer stockCountDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StockCountID", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "BatchNumber", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "SystemQuantity", nullable = false)
    private Integer systemQuantity;

    @Column(name = "ActualQuantity", nullable = false)
    private Integer actualQuantity;

    @Column(name = "Difference", nullable = false)
    private Integer difference;

    @Column(name = "Reason", length = 255)
    private String reason;
}
