package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "goods_receipt_details")
@Data
public class GoodsReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goods_receipt_detail_id")
    private Integer receiptDetailId;

    @Column(name = "goods_receipt_id", nullable = false)
    private Integer goodsReceiptId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "batch_number", nullable = false, length = 60)
    private String batchNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity = 0;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity = 0;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Column(name = "actual_unit_price", nullable = false)
    private Integer actualUnitPrice = 0;

    @Column(name = "inspection_result", nullable = false, length = 30)
    private String inspectionResult = "PASS";

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(255)")
    private String rejectionReason;

    // Navigation cho service impl
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", insertable = false, updatable = false)
    private Medicine medicine;
}
