package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.SystemSetting;

import java.math.BigDecimal;
import java.util.List;

public interface SystemSettingService {
    List<SystemSetting> getAllSettings();
    SystemSetting saveSetting(SystemSetting setting);
    SystemSetting getSettingById(Integer id);
    void deleteSetting(Integer id);
    BigDecimal getMoneyLimit(String key, BigDecimal defaultValue);
    Integer getIntegerValue(String key, Integer defaultValue);
    String getValue(String key, String defaultValue);
}
