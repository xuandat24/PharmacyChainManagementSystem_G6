package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Data
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(120)")
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== Helper methods (roleId: 1=ADMIN, 2=BRANCH_MANAGER, 3=PHARMACIST) =====

    @Transient
    public boolean isAdmin() { return Integer.valueOf(1).equals(roleId); }

    @Transient
    public boolean isBranchManager() { return Integer.valueOf(2).equals(roleId); }

    @Transient
    public boolean isPharmacist() { return Integer.valueOf(3).equals(roleId); }

    /** String role để P2 controllers dùng getRole() không lỗi */
    @Transient
    public String getRole() {
        if (isAdmin())         return "Admin";
        if (isBranchManager()) return "BranchManager";
        if (isPharmacist())    return "Pharmacist";
        return "Unknown";
    }

    @Transient

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}