package fu.se.pharmacy.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO nhận dữ liệu từ form ghi nhận đơn thuốc.
 */
@Data
public class PrescriptionDTO {

    private Integer prescriptionId;
    @NotNull(message = "Vui lòng chọn khách hàng")
    private Integer customerId;
    private String customerName;
    private String prescriptionCode;
    private String doctorName;
    private String clinicName;
    @NotNull(message = "Ngày kê đơn không được để trống")
    private LocalDate prescriptionDate;
    private LocalDate validUntil;
    private boolean valid;
    private String note;
    private LocalDateTime createdAt;
    private List<PrescriptionDetailDTO> details;
}