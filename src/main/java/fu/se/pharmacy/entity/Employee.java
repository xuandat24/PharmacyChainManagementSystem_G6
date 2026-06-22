package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "app_users") 
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") // Tên cột trong SQL là user_id
    private Integer employeeId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Trong SQL, Role được lưu bằng role_id (số nguyên) trỏ sang bảng roles
    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    // Thêm các cột bắt buộc khác để JPA không báo lỗi khi thao tác
    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";
}