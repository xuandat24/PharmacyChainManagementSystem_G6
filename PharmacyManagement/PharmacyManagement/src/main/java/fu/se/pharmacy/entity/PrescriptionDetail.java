package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "prescription_details")
@Data
public class PrescriptionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prescription_detail_id")
    private Integer prescriptionDetailId;

    @Column(name = "prescription_id", nullable = false)
    private Integer prescriptionId;

    @Column(name = "medicine_id", nullable = false)
    private Integer medicineId;

    @Column(name = "prescribed_quantity", nullable = false)
    private Integer prescribedQuantity;

    @Column(name = "dosage_instruction")
    private String dosageInstruction;
}