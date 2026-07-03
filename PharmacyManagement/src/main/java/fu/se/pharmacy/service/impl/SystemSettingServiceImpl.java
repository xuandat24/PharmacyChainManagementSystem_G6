package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.SystemSetting;
import fu.se.pharmacy.repository.SystemSettingRepository;
import fu.se.pharmacy.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repository;

    @Override public List<SystemSetting> getAllSettings() { return repository.findAll(); }
    @Override public SystemSetting saveSetting(SystemSetting setting) { return repository.save(setting); }
    @Override public SystemSetting getSettingById(Integer id) { return repository.findById(id).orElse(null); }

    // FIX: thêm deleteSetting - controller cần nhưng interface chưa có
    @Override
    public void deleteSetting(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public BigDecimal getMoneyLimit(String key, BigDecimal defaultValue) {
        return repository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> { try { return new BigDecimal(v.trim()); } catch (Exception e) { return defaultValue; } })
                .orElse(defaultValue);
    }

    @Override
    public Integer getIntegerValue(String key, Integer defaultValue) {
        return repository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .map(v -> { try { return (int) Double.parseDouble(v.trim()); } catch (Exception e) { return defaultValue; } })
                .orElse(defaultValue);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }
}
