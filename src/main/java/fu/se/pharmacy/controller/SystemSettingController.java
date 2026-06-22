package fu.se.pharmacy.controller;
import fu.se.pharmacy.dto.SystemSettingDTO;
import fu.se.pharmacy.entity.SystemSetting;
import fu.se.pharmacy.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService service;

    @GetMapping
    public String listSettings(Model model) {
        model.addAttribute("settings", service.getAllSettings());
        return "settings/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("setting", new SystemSettingDTO());
        return "settings/form";
    }

    @PostMapping("/save")
    public String saveSetting(@ModelAttribute("setting") SystemSettingDTO dto) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingId(dto.getSettingId());
        setting.setSettingKey(dto.getSettingKey());
        setting.setSettingValue(dto.getSettingValue());
        setting.setDescription(dto.getDescription());
        setting.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        service.saveSetting(setting);
        return "redirect:/settings";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        SystemSetting setting = service.getSettingById(id);
        if (setting != null) {
            SystemSettingDTO dto = new SystemSettingDTO();
            dto.setSettingId(setting.getSettingId());
            dto.setSettingKey(setting.getSettingKey());
            dto.setSettingValue(setting.getSettingValue());
            dto.setDescription(setting.getDescription());
            dto.setStatus(setting.getStatus());
            model.addAttribute("setting", dto);
            return "settings/form";
        }
        return "redirect:/settings";
    }

    @GetMapping("/delete/{id}")
    public String deleteSetting(@PathVariable("id") Integer id) {
        service.deleteSetting(id);
        return "redirect:/settings";
    }
}