package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "suppliers")
@Data
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "supplier_code", nullable = false, unique = true, length = 40)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, columnDefinition = "NVARCHAR(180)")
    private String supplierName;

    @Column(name = "supplier_type", nullable = false, length = 40)
    private String supplierType;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "address", columnDefinition = "NVARCHAR(255)")
    private String address;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "contact_person", columnDefinition = "NVARCHAR(120)")
    private String contactPerson;

    @Column(name = "license_no", length = 80)
    private String licenseNo;

    // FIX: DB CHECK constraint chỉ chấp nhận: DRAFT, APPROVED, SUSPENDED, INACTIVE
    // PENDING không hợp lệ → đổi default sang DRAFT
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";
}
