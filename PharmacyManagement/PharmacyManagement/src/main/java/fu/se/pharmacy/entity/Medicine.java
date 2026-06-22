package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "Medicine")
@Data
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MedicineID")
    private Integer medicineId;

    @Column(name = "MedicineName", nullable = false, length = 150)
    private String medicineName;

    @Column(name = "GenericName", length = 150)
    private String genericName;

    @Column(name = "Strength", length = 50)
    private String strength;

    @Column(name = "DosageForm", length = 50)
    private String dosageForm;

    @Column(name = "Barcode", unique = true, length = 50)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SupplierID", nullable = false)
    private Supplier supplier;

    @Column(name = "UnitPrice", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "RequiresPrescription", nullable = false)
    private Boolean requiresPrescription = false;

    @Column(name = "ReorderLevel", nullable = false)
    private Integer reorderLevel = 10;
}
