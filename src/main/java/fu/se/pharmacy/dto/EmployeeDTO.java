package fu.se.pharmacy.dto;

import lombok.Data;

@Data
public class EmployeeDTO {
    private Integer employeeId;
    private String fullName;
    private String username;
    private String passwordHash;
    private Integer roleId;   
    private Integer branchId;
    private String status;
}