package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "branches")
@Data
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "branch_code", nullable = false, unique = true, length = 30)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}