package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Employee")
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EmployeeID")
    private Integer employeeId;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Username", nullable = false, unique = true)
    private String username;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "Role", nullable = false)
    private String role;
}