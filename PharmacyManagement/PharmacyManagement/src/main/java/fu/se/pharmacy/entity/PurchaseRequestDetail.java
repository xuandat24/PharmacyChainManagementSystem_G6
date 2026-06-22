package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "PurchaseRequestDetail")
@Data
public class PurchaseRequestDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestDetailID")
    private Integer requestDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequestID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MedicineID", nullable = false)
    private Medicine medicine;

    @Column(name = "QuantityRequested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "QuantityApproved", nullable = false)
    private Integer quantityApproved = 0;

    @Column(name = "EstimatedPrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedPrice;
}
