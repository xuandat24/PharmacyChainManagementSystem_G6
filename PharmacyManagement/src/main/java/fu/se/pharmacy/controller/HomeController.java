package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.Medicine;
import fu.se.pharmacy.repository.AppUserRepository;
import fu.se.pharmacy.repository.BranchRepository;
import fu.se.pharmacy.repository.InventoryBatchRepository;
import fu.se.pharmacy.repository.MedicineRepository;
import fu.se.pharmacy.repository.SaleRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BranchRepository branchRepository;
    private final MedicineRepository medicineRepository;
    private final AppUserRepository appUserRepository;
    private final SaleRepository saleRepository;
    private final InventoryBatchRepository inventoryBatchRepository;

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        // FIX: ép sang long tường minh - tránh Spring Data wrap thành Optional<Long>
        model.addAttribute("totalBranches", (long) branchRepository.countByStatus("ACTIVE"));
        model.addAttribute("totalMedicines", (long) medicineRepository.countByStatus("ACTIVE"));
        model.addAttribute("totalUsers",    (long) appUserRepository.countByStatus("ACTIVE"));

        // Doanh thu hôm nay từ DB thật
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(23, 59, 59);
        model.addAttribute("todaySales",
                saleRepository.countBySaleDateBetweenAndStatus(startOfDay, endOfDay, "COMPLETED"));

        // Số lô sắp hết hạn (< 90 ngày) - thay vì hardcode "8"
        LocalDate warningDate = LocalDate.now().plusDays(90);
        long expiringCount = inventoryBatchRepository.findAll().stream()
                .filter(b -> "AVAILABLE".equals(b.getStatus())
                        && b.getExpiryDate() != null
                        && !b.getExpiryDate().isBefore(LocalDate.now())
                        && !b.getExpiryDate().isAfter(warningDate))
                .count();
        model.addAttribute("expiringCount", expiringCount);

        // Top thuốc đang bán (giới hạn 10) thay vì bảng dummy rỗng
        List<Medicine> topMedicines = medicineRepository.findByStatus("ACTIVE")
                .stream().limit(10).toList();
        model.addAttribute("medicines", topMedicines);

        model.addAttribute("user", user);
        return "index";
    }
}
