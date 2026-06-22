package fu.se.pharmacy.dto;

import lombok.Data;

@Data
public class BranchDTO {
    private Integer branchId;
    private String branchCode;
    private String branchName;
    private String address;
    private String phone;
    private String status;
}