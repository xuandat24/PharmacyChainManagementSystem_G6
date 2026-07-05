package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.InventoryBatch;
import fu.se.pharmacy.repository.InventoryBatchRepository;
import fu.se.pharmacy.repository.InventoryTransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 2 - Tồn kho):
 * "Xem tồn toàn hệ thống đối với Admin. Xem tồn chi nhánh đối với Manager.
 *  Xem tồn khả dụng đối với Pharmacist."
 * → Mọi role đã đăng nhập đều XEM được tồn kho (chỉ khác phạm vi: Admin toàn hệ thống,
 *   Manager/Pharmacist theo chi nhánh của mình). Không có hành động ghi/sửa ở đây vì
 *   "Không ai được sửa trực tiếp số lượng tồn ngoài InventoryService".
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private InventoryTransactionRepository transactionRepository;

    @GetMapping
    public String showInventory(HttpSession session,
                                @RequestParam(required = false) String search,
                                @RequestParam(required = false) String filter,
                                Model model) {
        // FIX: dùng AuthInterceptor.requireLogin() thay vì tự lấy session thủ công,
        // đồng bộ cách kiểm tra đăng nhập với toàn bộ hệ thống
        AppUser user = AuthInterceptor.requireLogin(session);

        Integer branchId = user.getBranchId();
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(90);

        List<InventoryBatch> stock;

        if ("expiring".equals(filter)) {
            stock = inventoryBatchRepository.findAll().stream()
                    .filter(b -> b.getExpiryDate() != null
                            && !b.getExpiryDate().isBefore(today)
                            && !b.getExpiryDate().isAfter(warningDate)
                            && (user.isAdmin() || b.getBranchId().equals(branchId)))
                    .collect(Collectors.toList());
        } else if ("expired".equals(filter)) {
            stock = inventoryBatchRepository.findAll().stream()
                    .filter(b -> b.getExpiryDate() != null
                            && b.getExpiryDate().isBefore(today)
                            && (user.isAdmin() || b.getBranchId().equals(branchId)))
                    .collect(Collectors.toList());
        } else if (search != null && !search.isBlank()) {
            stock = inventoryBatchRepository.findAll().stream()
                    .filter(b -> b.getBatchNumber().toLowerCase().contains(search.toLowerCase())
                            && (user.isAdmin() || b.getBranchId().equals(branchId)))
                    .collect(Collectors.toList());
        } else {
            if (user.isAdmin()) {
                stock = inventoryBatchRepository.findAll().stream()
                        .filter(b -> "AVAILABLE".equals(b.getStatus()))
                        .collect(Collectors.toList());
            } else {
                stock = inventoryBatchRepository.findAll().stream()
                        .filter(b -> b.getBranchId().equals(branchId)
                                && "AVAILABLE".equals(b.getStatus()))
                        .collect(Collectors.toList());
            }
        }

        long totalStock = stock.stream().mapToLong(b -> b.getQuantityOnHand() == null ? 0 : b.getQuantityOnHand()).sum();

        List<InventoryBatch> expiring = inventoryBatchRepository.findAll().stream()
                .filter(b -> b.getExpiryDate() != null
                        && !b.getExpiryDate().isBefore(today)
                        && !b.getExpiryDate().isAfter(warningDate)
                        && (user.isAdmin() || (branchId != null && b.getBranchId().equals(branchId))))
                .collect(Collectors.toList());

        List<InventoryBatch> expired = inventoryBatchRepository.findAll().stream()
                .filter(b -> b.getExpiryDate() != null
                        && b.getExpiryDate().isBefore(today)
                        && (user.isAdmin() || (branchId != null && b.getBranchId().equals(branchId))))
                .collect(Collectors.toList());

        model.addAttribute("stock", stock);
        model.addAttribute("search", search);
        model.addAttribute("filter", filter);
        model.addAttribute("user", user);
        model.addAttribute("totalStock", totalStock);
        model.addAttribute("expiringCount", expiring.size());
        model.addAttribute("expiredCount", expired.size());

        return "inventory/index";
    }

    @GetMapping("/transactions")
    public String showTransactions(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);

        if (user.isAdmin()) {
            model.addAttribute("transactions", transactionRepository.findAll());
        } else {
            model.addAttribute("transactions",
                    transactionRepository.findByBranchId(user.getBranchId()));
        }
        model.addAttribute("user", user);
        return "inventory/transactions";
    }
}
