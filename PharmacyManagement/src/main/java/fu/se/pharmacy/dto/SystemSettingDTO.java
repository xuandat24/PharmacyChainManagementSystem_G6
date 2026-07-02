package fu.se.pharmacy.dto;
import lombok.Data;

import java.util.Date;

@Data
public class SystemSettingDTO {
    private Integer settingId;
    private String settingKey;
    private String settingValue;
    private String description;
    private Date updatedAt;
}