package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "medicines")
@Data
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medicine_id")
    private Integer medicineId;

    @Column(name = "medicine_code", nullable = false, unique = true, length = 30)
    private String medicineCode;

    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    // Khóa ngoại trỏ sang bảng Category
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "unit", length = 50)
    private String unit; // Đơn vị tính: Hộp, Vỉ, Viên...

    @Column(name = "price")
    private Double price;

    @Column(name = "description")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";
}