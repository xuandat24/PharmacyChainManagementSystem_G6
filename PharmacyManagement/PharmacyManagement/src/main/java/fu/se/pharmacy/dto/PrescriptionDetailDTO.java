package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO cho một dòng thuốc trong đơn thuốc.
 */
@Data
public class PrescriptionDetailDTO {

    private Integer prescriptionDetailId;
    private Integer prescriptionId;

    @NotNull(message = "Vui lòng chọn thuốc")
    private Integer medicineId;

    private String medicineName; // output only
    private String medicineUnit; // output only

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1,    message = "Số lượng tối thiểu là 1")
    @Max(value = 9999, message = "Số lượng tối đa là 9999")
    private Integer prescribedQuantity;

    @Size(max = 255, message = "Hướng dẫn sử dụng tối đa 255 ký tự")
    private String dosageInstruction;
}