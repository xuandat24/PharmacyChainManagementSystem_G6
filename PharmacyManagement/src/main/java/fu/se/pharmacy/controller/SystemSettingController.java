package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.SystemSettingDTO;
import fu.se.pharmacy.entity.SystemSetting;
import fu.se.pharmacy.service.SystemSettingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService service;

    @GetMapping
    public String listSettings(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("settings", service.getAllSettings());
        return "settings/list";
    }

    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        model.addAttribute("setting", new SystemSettingDTO());
        return "settings/form";
    }

    @PostMapping("/save")
    public String saveSetting(@ModelAttribute("setting") SystemSettingDTO dto,
                              HttpSession session, RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        SystemSetting setting = new SystemSetting();
        setting.setSettingId(dto.getSettingId());
        setting.setSettingKey(dto.getSettingKey());
        setting.setSettingValue(dto.getSettingValue());
        setting.setDescription(dto.getDescription());
        setting.setUpdatedAt(dto.getUpdatedAt());
        service.saveSetting(setting);
        ra.addFlashAttribute("success", "Da cap nhat cau hinh he thong");
        return "redirect:/settings";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, HttpSession session, Model model) {
        AuthInterceptor.requireRole(session, "Admin");
        SystemSetting setting = service.getSettingById(id);
        if (setting != null) {
            SystemSettingDTO dto = new SystemSettingDTO();
            dto.setSettingId(setting.getSettingId());
            dto.setSettingKey(setting.getSettingKey());
            dto.setSettingValue(setting.getSettingValue());
            dto.setDescription(setting.getDescription());
            dto.setUpdatedAt(setting.getUpdatedAt());
            model.addAttribute("setting", dto);
            return "settings/form";
        }
        return "redirect:/settings";
    }



}
