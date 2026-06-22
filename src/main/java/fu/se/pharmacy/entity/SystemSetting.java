package fu.se.pharmacy.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "system_settings")
@Data
public class SystemSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    @Column(name = "description")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";
}