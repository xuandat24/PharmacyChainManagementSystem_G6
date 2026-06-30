package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.EmployeeDTO;
import fu.se.pharmacy.entity.Employee;
import fu.se.pharmacy.service.BranchService;
import fu.se.pharmacy.service.EmployeeService;
import fu.se.pharmacy.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final BranchService branchService;
    private final RoleService roleService;

    @GetMapping
    public String listEmployees(Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees());
        return "employees/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("employee", new EmployeeDTO());
        // Bơm dữ liệu cho 2 ô Dropdown
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("roles", roleService.getAllRoles());
        return "employees/form";
    }

    @PostMapping("/save")
    public String saveEmployee(@ModelAttribute("employee") EmployeeDTO dto, Model model) {
        Employee emp = new Employee();
        emp.setEmployeeId(dto.getEmployeeId());
        emp.setFullName(dto.getFullName());
        emp.setUsername(dto.getUsername());

        // Nếu là sửa và không nhập pass mới thì giữ nguyên (logic cơ bản)
        if (dto.getPasswordHash() != null && !dto.getPasswordHash().isEmpty()) {
            emp.setPasswordHash(dto.getPasswordHash());
        }

        emp.setRoleId(dto.getRoleId());
        emp.setBranchId(dto.getBranchId());
        emp.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        try {
            // Cố gắng lưu dữ liệu
            employeeService.saveEmployee(emp);
            return "redirect:/employees";

        } catch (DataIntegrityViolationException e) {
            // BẮT LỖI TRÙNG LẶP: Nếu Username đã tồn tại, code sẽ nhảy vào đây
            model.addAttribute("error", "Tên đăng nhập này đã tồn tại! Vui lòng chọn tên khác.");

            // Trả lại các dữ liệu vừa nhập để người dùng không phải gõ lại
            model.addAttribute("employee", dto);

            // QUAN TRỌNG: Phải bơm lại danh sách dropdown, nếu không form sẽ bị lỗi
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("roles", roleService.getAllRoles());

            // Trả về lại trang form thêm nhân viên
            return "employees/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Employee emp = employeeService.getEmployeeById(id);
        if (emp != null) {
            EmployeeDTO dto = new EmployeeDTO();
            dto.setEmployeeId(emp.getEmployeeId());
            dto.setFullName(emp.getFullName());
            dto.setUsername(emp.getUsername());
            dto.setPasswordHash(emp.getPasswordHash());
            dto.setRoleId(emp.getRoleId());
            dto.setBranchId(emp.getBranchId());
            dto.setStatus(emp.getStatus());

            model.addAttribute("employee", dto);
            model.addAttribute("branches", branchService.getAllBranches());
            model.addAttribute("roles", roleService.getAllRoles());
            return "employees/form";
        }
        return "redirect:/employees";
    }

    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable("id") Integer id) {
        employeeService.deleteEmployee(id);
        return "redirect:/employees";
    }
}