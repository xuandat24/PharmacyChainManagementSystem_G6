package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.Employee;
import fu.se.pharmacy.repository.EmployeeRepository;
import fu.se.pharmacy.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;

    @Override
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public Employee saveEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Override
    public Employee getEmployeeById(Integer id) {
        return employeeRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteEmployee(Integer id) {
        Employee emp = getEmployeeById(id);
        if (emp != null) {
            if ("admin".equalsIgnoreCase(emp.getUsername())) {
                throw new RuntimeException("Cảnh báo bảo mật: Không được phép khóa tài khoản Admin hệ thống!");
            }

            emp.setStatus("INACTIVE");
            employeeRepository.save(emp);
        }
    }
}