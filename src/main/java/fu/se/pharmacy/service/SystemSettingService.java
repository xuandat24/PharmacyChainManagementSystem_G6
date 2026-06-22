package fu.se.pharmacy.service;
import fu.se.pharmacy.entity.SystemSetting;
import java.util.List;

public interface SystemSettingService {
    List<SystemSetting> getAllSettings();
    SystemSetting saveSetting(SystemSetting setting);
    SystemSetting getSettingById(Integer id);
    void deleteSetting(Integer id);
}