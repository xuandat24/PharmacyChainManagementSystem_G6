package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Data
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_id")
    private Integer prescriptionId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "prescription_code", length = 50)
    private String prescriptionCode;

    @Column(name = "doctor_name", columnDefinition = "NVARCHAR(120)")
    private String doctorName;

    @Column(name = "clinic_name", columnDefinition = "NVARCHAR(200)")
    private String clinicName;

    @Column(name = "prescription_date")
    private LocalDate prescriptionDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public boolean isValid() {
        return validUntil == null || !LocalDate.now().isAfter(validUntil);
    }

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}