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

    @Column(name = "medicine_code", nullable = false, unique = true)
    private String medicineCode;

    @Column(name = "barcode")
    private String barcode;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "active_ingredient")
    private String activeIngredient;

    @Column(name = "strength")
    private String strength;

    @Column(name = "dosage_form")
    private String dosageForm;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "manufacturer")
    private String manufacturer;

    @Column(name = "country_of_origin")
    private String countryOfOrigin;

    @Column(name = "sale_price", nullable = false)
    private Integer salePrice = 0;

    @Column(name = "min_stock_level", nullable = false)
    private Integer minStockLevel = 0;

    @Column(name = "requires_prescription", nullable = false)
    private Boolean requiresPrescription = false;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}