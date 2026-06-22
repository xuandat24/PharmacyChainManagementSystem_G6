package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.Employee;
import fu.se.pharmacy.entity.Inventory;
import fu.se.pharmacy.repository.InventoryRepository;
import fu.se.pharmacy.repository.InventoryTransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @GetMapping
    public String showInventory(HttpSession session,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "filter", required = false) String filter,
                                Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<Inventory> stock;
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(90); // Hạn mức cảnh báo 90 ngày

        boolean isAdmin = "Admin".equals(user.getRole());
        Integer branchId = user.getBranch().getBranchId();

        // 1. Thực hiện tìm kiếm và lọc dữ liệu
        if (search != null && !search.trim().isEmpty()) {
            if (isAdmin) {
                stock = inventoryRepository.searchStockSystemWide(search.trim());
            } else {
                stock = inventoryRepository.searchStock(branchId, search.trim());
            }
        } else if ("expiring".equals(filter)) {
            if (isAdmin) {
                stock = inventoryRepository.findExpiringSoonSystemWide(today, warningDate);
            } else {
                stock = inventoryRepository.findExpiringSoon(branchId, today, warningDate);
            }
        } else if ("expired".equals(filter)) {
            if (isAdmin) {
                stock = inventoryRepository.findExpiredSystemWide(today);
            } else {
                stock = inventoryRepository.findExpired(branchId, today);
            }
        } else {
            // Mặc định xem tồn kho
            if (isAdmin) {
                stock = inventoryRepository.findAll();
            } else if ("Pharmacist".equals(user.getRole())) {
                stock = inventoryRepository.findAvailableStock(branchId, today);
            } else {
                stock = inventoryRepository.findByBranch_BranchId(branchId);
            }
        }

        model.addAttribute("stock", stock);
        model.addAttribute("search", search);
        model.addAttribute("filter", filter);
        model.addAttribute("user", user);

        // Tính các chỉ số thống kê hiển thị nhanh trên Dashboard Kho
        long totalStock = stock.stream().mapToLong(Inventory::getQuantity).sum();
        long expiringCount = isAdmin 
                ? inventoryRepository.findExpiringSoonSystemWide(today, warningDate).size()
                : inventoryRepository.findExpiringSoon(branchId, today, warningDate).size();
        long expiredCount = isAdmin
                ? inventoryRepository.findExpiredSystemWide(today).size()
                : inventoryRepository.findExpired(branchId, today).size();

        model.addAttribute("totalStock", totalStock);
        model.addAttribute("expiringCount", expiringCount);
        model.addAttribute("expiredCount", expiredCount);

        return "inventory/index";
    }

    @GetMapping("/transactions")
    public String showTransactions(HttpSession session, Model model) {
        Employee user = (Employee) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if ("Admin".equals(user.getRole())) {
            model.addAttribute("transactions", transactionRepository.findAll());
        } else {
            model.addAttribute("transactions", transactionRepository.findByBranch_BranchId(user.getBranch().getBranchId()));
        }

        model.addAttribute("user", user);
        return "inventory/transactions";
    }
}
