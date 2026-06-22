package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Inventory", uniqueConstraints = {
    @UniqueConstraint(name = "UQ_Inventory_Batch", columnNames = {"BranchID", "MedicineID", "BatchNumber"})
})
@Data
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InventoryID")
    private Integer inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "BatchNumber", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "ManufactureDate")
    private LocalDate manufactureDate;

    @Column(name = "ExpiryDate")
    private LocalDate expiryDate;

    @Column(name = "PurchasePrice", precision = 12, scale = 2)
    private BigDecimal purchasePrice;
}
