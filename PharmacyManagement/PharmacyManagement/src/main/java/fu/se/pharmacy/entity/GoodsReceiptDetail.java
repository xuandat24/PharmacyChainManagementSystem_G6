package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "GoodsReceiptDetail")
@Data
public class GoodsReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReceiptDetailID")
    private Integer receiptDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ReceiptID", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "BatchNumber", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "ExpiryDate", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "ManufactureDate")
    private LocalDate manufactureDate;

    @Column(name = "QuantityOrdered", nullable = false)
    private Integer quantityOrdered;

    @Column(name = "QuantityReceived", nullable = false)
    private Integer quantityReceived;

    @Column(name = "PurchasePrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;
}
