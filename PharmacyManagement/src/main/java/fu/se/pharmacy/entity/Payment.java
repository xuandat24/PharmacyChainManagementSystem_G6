package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "sale_id", nullable = false)
    private Integer saleId;

    @Column(name = "payment_code", nullable = false, unique = true)
    private String paymentCode;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // CASH, ONLINE

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, PAID, FAILED, CANCELLED, REFUNDED

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "customer_paid_amount")
    private Integer customerPaidAmount;

    @Column(name = "change_amount")
    private Integer changeAmount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "note")
    private String note;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.paymentCode == null)
            this.paymentCode = "PAY-" + System.currentTimeMillis();
    }
}