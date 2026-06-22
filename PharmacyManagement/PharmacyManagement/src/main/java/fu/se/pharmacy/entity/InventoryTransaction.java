package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "InventoryTransaction")
@Data
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BranchID", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "BatchNumber", nullable = false, length = 50)
    private String batchNumber;

    @Column(name = "TransactionType", nullable = false, length = 30)
    private String transactionType; // RECEIPT, SALE, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT_IN, ADJUSTMENT_OUT, SALE_RETURN, EXPIRED_DISPOSAL

    @Column(name = "ReferenceID", nullable = false)
    private Integer referenceId;

    @Column(name = "QuantityChange", nullable = false)
    private Integer quantityChange;

    @Column(name = "TransactionDate", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByID", nullable = false)
    private Employee createdBy;
}
