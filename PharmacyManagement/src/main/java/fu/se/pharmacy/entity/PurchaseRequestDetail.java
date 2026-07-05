package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "purchase_request_details")
@Data
public class PurchaseRequestDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_request_detail_id")
    private Integer purchaseRequestDetailId;

    @Column(name = "purchase_request_id", nullable = false)
    private Integer purchaseRequestId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    // FIX: PurchaseRequestServiceImpl gọi getRequestedQuantity() - đổi tên cho nhất quán
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    // FIX: PurchaseRequestServiceImpl gọi getApprovedQuantity()/setApprovedQuantity()
    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    // FIX: PurchaseRequestServiceImpl gọi getExpectedUnitPrice()
    @Column(name = "expected_unit_price", nullable = false)
    private Integer expectedUnitPrice = 0;

    @Column(name = "note", columnDefinition = "NVARCHAR(255)")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", insertable = false, updatable = false)
    private Medicine medicine;
}