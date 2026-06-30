package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.BranchDTO;
import fu.se.pharmacy.entity.Branch;
import fu.se.pharmacy.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/branches")
@RequiredArgsConstructor // 1. Tự động tạo Constructor Injection (Fix lỗi S6813)
public class BranchController {

    private final BranchService branchService; // Thêm chữ 'final' và bỏ @Autowired

    @GetMapping
    public String listBranches(Model model) {
        model.addAttribute("branches", branchService.getAllBranches());
        return "branches/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        // 2. Ném DTO rỗng ra form thay vì Entity (Fix lỗi S4684)
        model.addAttribute("branch", new BranchDTO());
        return "branches/form";
    }

    @PostMapping("/save")
    public String saveBranch(@ModelAttribute("branch") BranchDTO branchDTO, Model model) {
        // 3. Hứng DTO từ form, sau đó Map (chuyển đổi) sang Entity
        Branch branch = new Branch();
        branch.setBranchId(branchDTO.getBranchId());
        branch.setBranchCode(branchDTO.getBranchCode());
        branch.setBranchName(branchDTO.getBranchName());
        branch.setAddress(branchDTO.getAddress());
        branch.setPhone(branchDTO.getPhone());

        // Nếu tạo mới chưa có status thì mặc định là ACTIVE
        branch.setStatus(branchDTO.getStatus() != null ? branchDTO.getStatus() : "ACTIVE");

        try {
            // 4. Cố gắng lưu Entity xuống DB
            branchService.saveBranch(branch);
            return "redirect:/branches"; // Lưu thành công thì về danh sách

        } catch (DataIntegrityViolationException e) {
            // BẮT LỖI TRÙNG LẶP: Nếu mã chi nhánh đã tồn tại, code sẽ nhảy vào đây
            model.addAttribute("error", "Đã có mã chi nhánh này rồi! Vui lòng nhập mã khác.");

            // Trả lại DTO về lại form để giữ nguyên các thông tin người dùng vừa nhập
            model.addAttribute("branch", branchDTO);

            // Trả lại giao diện form thêm chi nhánh
            return "branches/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Branch branch = branchService.getBranchById(id);
        if (branch != null) {
            BranchDTO dto = new BranchDTO();
            dto.setBranchId(branch.getBranchId());
            dto.setBranchCode(branch.getBranchCode());
            dto.setBranchName(branch.getBranchName());
            dto.setAddress(branch.getAddress());
            dto.setPhone(branch.getPhone());
            dto.setStatus(branch.getStatus());

            model.addAttribute("branch", dto);
            return "branches/form";
        }
        return "redirect:/branches";
    }


    @GetMapping("/delete/{id}")
    public String deleteBranch(@PathVariable("id") Integer id) {
        branchService.deleteBranch(id);
        return "redirect:/branches";
    }
}