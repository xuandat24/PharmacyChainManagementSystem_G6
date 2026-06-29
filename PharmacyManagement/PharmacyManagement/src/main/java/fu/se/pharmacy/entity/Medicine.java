package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicines")
@Data
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicine_id")
    private Integer medicineId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "medicine_code", nullable = false, unique = true, length = 50)
    private String medicineCode;

    @Column(name = "barcode", length = 50)
    private String barcode;

    // NVARCHAR vì tên thuốc tiếng Việt có dấu
    @Column(name = "medicine_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String medicineName;

    @Column(name = "active_ingredient", columnDefinition = "NVARCHAR(255)")
    private String activeIngredient;

    @Column(name = "strength", length = 50)
    private String strength;

    @Column(name = "dosage_form", columnDefinition = "NVARCHAR(100)")
    private String dosageForm;

    @Column(name = "unit", nullable = false, columnDefinition = "NVARCHAR(30)")
    private String unit;

    @Column(name = "manufacturer", columnDefinition = "NVARCHAR(200)")
    private String manufacturer;

    @Column(name = "country_of_origin", columnDefinition = "NVARCHAR(100)")
    private String countryOfOrigin;

    @Column(name = "sale_price", nullable = false)
    private Integer salePrice = 0;

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 0;

    @Column(name = "requires_prescription", nullable = false)
    private Boolean requiresPrescription = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}