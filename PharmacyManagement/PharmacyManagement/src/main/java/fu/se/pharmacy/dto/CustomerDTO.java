package fu.se.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO nhận dữ liệu từ form tạo/sửa khách hàng.
 */
@Data
public class CustomerDTO {

    private Integer customerId; // null khi tạo mới, có giá trị khi cập nhật

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    private LocalDate dateOfBirth;

    private String gender; // MALE, FEMALE, OTHER

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    private String allergyNote;

    private LocalDateTime createdAt;
}