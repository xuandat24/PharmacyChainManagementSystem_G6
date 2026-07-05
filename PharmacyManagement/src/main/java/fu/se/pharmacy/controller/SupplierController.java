package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.entity.Supplier;
import fu.se.pharmacy.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    @Autowired private SupplierService supplierService;

    @GetMapping
    public String listSuppliers(@RequestParam(value = "search", required = false) String search,
                                HttpSession session,
                                Model model) {
        AppUser user = AuthInterceptor.requireLogin(session);
        model.addAttribute("suppliers", supplierService.searchSuppliers(search));
        model.addAttribute("search", search);
        model.addAttribute("user", user);
        return "suppliers/list";
    }

    @GetMapping("/create")
    public String showCreateForm(HttpSession session, Model model) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("user", user);
        return "suppliers/create";
    }

    @PostMapping("/save")
    public String saveSupplier(HttpSession session,
                               @Valid @ModelAttribute("supplier") Supplier supplier,
                               BindingResult result,
                               Model model) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "suppliers/create";
        }

        // FIX: bắt lỗi trùng mã NCC (supplier_code UNIQUE) / số điện thoại / email...
        // thay vì để DataIntegrityViolationException ném thẳng ra ngoài gây 500 Whitelabel.
        try {
            supplierService.saveSupplier(supplier);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("user", user);
            model.addAttribute("error", buildDuplicateMessage(e, supplier.getSupplierCode()));
            return "suppliers/create";
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               HttpSession session,
                               Model model) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        model.addAttribute("user", user);
        return "suppliers/edit";
    }

    @PostMapping("/update/{id}")
    public String updateSupplier(@PathVariable Integer id,
                                 HttpSession session,
                                 @Valid @ModelAttribute("supplier") Supplier supplier,
                                 BindingResult result,
                                 Model model) {
        AppUser user = AuthInterceptor.requireRole(session, "Admin", "BranchManager");
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            return "suppliers/edit";
        }
        supplier.setSupplierId(id);

        // FIX: tương tự cho update — tránh trùng mã NCC với NCC khác
        try {
            supplierService.saveSupplier(supplier);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("user", user);
            model.addAttribute("error", buildDuplicateMessage(e, supplier.getSupplierCode()));
            return "suppliers/edit";
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Integer id, HttpSession session,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Admin");
        // FIX: Supplier đang được PurchaseRequest/InventoryBatch tham chiếu (FK)
        // sẽ ném DataIntegrityViolationException nếu xóa cứng → trước đây không bắt,
        // gây crash 500 Whitelabel khi Admin xóa NCC đã có giao dịch.
        try {
            supplierService.deleteSupplier(id);
            ra.addFlashAttribute("success", "Đã xóa nhà cung cấp");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error",
                    "Không thể xóa nhà cung cấp này vì đã có yêu cầu nhập hàng hoặc lô hàng liên kết. "
                            + "Hãy chuyển trạng thái sang INACTIVE thay vì xóa.");
        }
        return "redirect:/suppliers";
    }

    /**
     * Dựng thông báo lỗi thân thiện từ DataIntegrityViolationException.
     * SQL Server trả message kỹ thuật dạng:
     *   "Violation of UNIQUE KEY constraint '...'. Cannot insert duplicate key ... (SUP001)."
     * → rút ra giá trị bị trùng để hiển thị cho người dùng.
     */
    private String buildDuplicateMessage(DataIntegrityViolationException e, String attemptedCode) {
        String raw = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        if (raw != null && raw.contains("UNIQUE KEY constraint")) {
            return "Mã nhà cung cấp \"" + attemptedCode + "\" đã tồn tại trong hệ thống. "
                    + "Vui lòng nhập mã khác.";
        }
        return "Không thể lưu nhà cung cấp do dữ liệu bị trùng hoặc không hợp lệ. "
                + "Vui lòng kiểm tra lại mã NCC, số điện thoại hoặc email.";
    }
}