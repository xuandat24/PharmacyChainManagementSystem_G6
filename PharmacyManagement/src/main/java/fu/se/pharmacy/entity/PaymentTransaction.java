package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_transaction_id")
    private Integer paymentTransactionId;

    @Column(name = "payment_id", nullable = false)
    private Integer paymentId;

    @Column(name = "gateway_transaction_code")
    private String gatewayTransactionCode;

    @Column(name = "transaction_status", nullable = false)
    private String transactionStatus = "PENDING"; // PENDING, SUCCESS, FAILED

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "raw_message")
    private String rawMessage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}