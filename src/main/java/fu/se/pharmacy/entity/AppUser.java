package fu.se.pharmacy.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    private String username;
    private String password;
    private String fullName;

 
    private Integer branchId;
    private Integer roleId;

    private String status = "ACTIVE";
}