package fu.se.pharmacy.dto;
import lombok.Data;

@Data
public class SystemSettingDTO {
    private Integer settingId;
    private String settingKey;
    private String settingValue;
    private String description;
    private String status;
}