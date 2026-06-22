package fu.se.pharmacy.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Customer")
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CustomerID")
    private Integer customerId;

    @Column(name = "FullName", nullable = false)
    private String fullName;

    @Column(name = "Phone")
    private String phone;

    @Column(name = "Address")
    private String address;

    @Column(name = "Email", unique = true)
    private String email;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "Gender")
    private String gender;

    @Column(name = "Allergies")
    private String allergies;

    @Column(name = "LoyaltyPoints", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt;

   
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}