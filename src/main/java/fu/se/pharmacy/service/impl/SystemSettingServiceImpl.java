package fu.se.pharmacy.service.impl;
import fu.se.pharmacy.entity.SystemSetting;
import fu.se.pharmacy.repository.SystemSettingRepository;
import fu.se.pharmacy.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository repository;

    @Override
    public List<SystemSetting> getAllSettings() {
        return repository.findAll();
    }

    @Override
    public SystemSetting saveSetting(SystemSetting setting) {
        return repository.save(setting);
    }

    @Override
    public SystemSetting getSettingById(Integer id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void deleteSetting(Integer id) {
        SystemSetting setting = getSettingById(id);
        if (setting != null) {
            setting.setStatus("INACTIVE");
            repository.save(setting);
        }
    }
}