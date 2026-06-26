package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Data
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Integer saleId;

    @Column(name = "sale_code", nullable = false, unique = true)
    private String saleCode;

    @Column(name = "branch_id", nullable = false)
    private Integer branchId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "prescription_id")
    private Integer prescriptionId;

    @Column(name = "pharmacist_id", nullable = false)
    private Integer pharmacistId;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "status", nullable = false)
    private String status = "DRAFT"; // DRAFT, COMPLETED, VOIDED, REFUNDED

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount = 0;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount = 0;

    @Column(name = "final_amount", nullable = false)
    private Integer finalAmount = 0;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        if (this.saleDate == null) this.saleDate = LocalDateTime.now();
        if (this.saleCode == null) this.saleCode = "SALE-" + System.currentTimeMillis();
    }
}