package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.Employee;
import java.util.List;

public interface EmployeeService {
    List<Employee> getAllEmployees();
    Employee saveEmployee(Employee employee);
    Employee getEmployeeById(Integer id);
    void deleteEmployee(Integer id);
}