package fu.se.pharmacy.dto;

import fu.se.pharmacy.validator.ValidDateRange;
import fu.se.pharmacy.validator.ValidGender;
import fu.se.pharmacy.validator.ValidPhone;
import fu.se.pharmacy.validator.VietnameseName;
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

    private Integer customerId; // null khi tạo mới

    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 120, message = "Họ tên phải từ 2 đến 120 ký tự")
    @VietnameseName(message = "Họ tên chỉ được chứa chữ cái và khoảng trắng")
    private String fullName;

    @ValidPhone
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @ValidDateRange(
            pastOrPresent = true,
            minYear = 1900,
            message = "Ngày sinh không hợp lệ"
    )
    private LocalDate dateOfBirth;

    @ValidGender
    private String gender; // MALE, FEMALE, OTHER

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Size(max = 255, message = "Ghi chú dị ứng tối đa 255 ký tự")
    private String allergyNote;

    private LocalDateTime createdAt;
}