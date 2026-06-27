package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO cho một dòng thuốc trong đơn thuốc.
 * Dùng cho cả input (thêm thuốc vào đơn) và output (hiển thị).
 */
@Data
public class PrescriptionDetailDTO {

    private Integer prescriptionDetailId;
    private Integer prescriptionId;

    @NotNull(message = "Vui lòng chọn thuốc")
    private Integer medicineId;

    private String medicineName;    // FIX: join từ medicines (output only)
    private String medicineUnit;    // FIX: đơn vị tính (output only)

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer prescribedQuantity;

    private String dosageInstruction;
}