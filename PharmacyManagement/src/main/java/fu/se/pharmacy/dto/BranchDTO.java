package fu.se.pharmacy.dto;

import fu.se.pharmacy.validator.ValidPhone;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BranchDTO {
    private Integer branchId;
    private String branchCode;
    private String branchName;
    private String address;

    @ValidPhone
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;
    private String status;
}