package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.SystemSetting;

import java.math.BigDecimal;
import java.util.List;

public interface SystemSettingService {
    List<SystemSetting> getAllSettings();
    SystemSetting saveSetting(SystemSetting setting);
    SystemSetting getSettingById(Integer id);

    /**
     * Đọc hạn mức tiền (MONEY) theo key, ví dụ PURCHASE_APPROVAL_LIMIT.
     * Nếu Admin chưa cấu hình trong bảng system_settings thì trả về defaultValue.
     */
    BigDecimal getMoneyLimit(String key, BigDecimal defaultValue);

    /**
     * Đọc giá trị số nguyên (INT) theo key, ví dụ EXPIRY_WARNING_DAYS.
     */
    Integer getIntegerValue(String key, Integer defaultValue);

    /**
     * Đọc giá trị chuỗi thô theo key.
     */
    String getValue(String key, String defaultValue);
}
