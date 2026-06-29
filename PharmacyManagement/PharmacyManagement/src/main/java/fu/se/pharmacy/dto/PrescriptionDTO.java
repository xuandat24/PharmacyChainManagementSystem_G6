package fu.se.pharmacy.dto;

import fu.se.pharmacy.validator.ValidDateRange;
import fu.se.pharmacy.validator.VietnameseName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PrescriptionDTO {

    private Integer prescriptionId;

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Integer customerId;

    private String customerName;

    @Size(max = 50, message = "Mã đơn thuốc tối đa 50 ký tự")
    private String prescriptionCode;

    @VietnameseName(allowDot = true, message = "Tên bác sĩ chỉ được chứa chữ cái, khoảng trắng và dấu chấm")
    @Size(max = 120, message = "Tên bác sĩ tối đa 120 ký tự")
    private String doctorName;

    @Size(max = 180, message = "Tên cơ sở khám tối đa 180 ký tự")
    private String clinicName;

    @NotNull(message = "Ngày kê đơn không được để trống")
    @ValidDateRange(
            pastOrPresent = true,
            minYear = 2000,
            message = "Ngày kê đơn không được ở tương lai và không được trước năm 2000"
    )
    private LocalDate prescriptionDate;

    // FIX: bỏ @ValidDateRange(futureOrPresent=true) — dược sĩ cần nhập lại đơn cũ
    // (đơn đã hết hạn vẫn được lưu vào hệ thống để lưu hồ sơ)
    // isValidForSale() sẽ kiểm tra hạn khi bán hàng, không cần block ở đây
    private LocalDate validUntil;

    private boolean valid;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String note;

    private LocalDateTime createdAt;
    private List<PrescriptionDetailDTO> details;
}