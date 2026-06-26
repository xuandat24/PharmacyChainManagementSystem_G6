package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Data
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_request_id")
    private Integer refundRequestId;

    @Column(name = "sale_id", nullable = false)
    private Integer saleId;

    @Column(name = "requested_by", nullable = false)
    private Integer requestedBy;

    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "refund_amount", nullable = false)
    private Integer refundAmount = 0;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED

    @PrePersist
    protected void onCreate() { this.requestedAt = LocalDateTime.now(); }
}