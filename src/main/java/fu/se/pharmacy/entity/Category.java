package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Data // Của thư viện Lombok giúp tự tạo Getter/Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, length = 120)
    private String categoryName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // Mặc định là ACTIVE như trong SQL
}