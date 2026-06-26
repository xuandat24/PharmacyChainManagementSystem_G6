package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO nhận yêu cầu thêm thuốc vào hóa đơn DRAFT.
 */
@Data
public class SaleDTO {

    @NotNull(message = "Mã hóa đơn không được để trống")
    private Integer saleId;

    @NotNull(message = "Vui lòng chọn thuốc")
    private Integer medicineId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer quantity;


        private String saleCode;
        private Integer branchId;
        private Integer pharmacistId;
        private String pharmacistName;    // join từ app_users

        private Integer customerId;
        private String customerName;      // join từ customers
        private String customerPhone;
        private String customerAllergyNote;

        private Integer prescriptionId;
        private String prescriptionCode;  // join từ prescriptions

        private LocalDateTime saleDate;
        private String status;
        private Integer totalAmount;
        private Integer discountAmount;
        private Integer finalAmount;
        private String note;

        private List<SaleDetailDTO> details;

}