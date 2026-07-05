package fu.se.pharmacy.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tổng hợp cho hóa đơn bán hàng.
 * Dùng làm response (hiển thị giỏ hàng, lịch sử, hóa đơn).
 * Request thêm thuốc dùng trực tiếp @RequestParam trong Controller.
 */
@Data
public class SaleDTO {

    private Integer saleId;
    private String saleCode;
    private Integer branchId;
    private Integer pharmacistId;
    private String pharmacistName;       // join từ app_users

    private Integer customerId;
    private String customerName;         // join từ customers
    private String customerPhone;
    private String customerAllergyNote;

    private Integer prescriptionId;
    private String prescriptionCode;     // join từ prescriptions

    private LocalDateTime saleDate;
    private String status;               // DRAFT, COMPLETED, VOIDED, REFUNDED
    private Integer totalAmount;
    private Integer discountAmount;
    private Integer finalAmount;
    private String note;

    private List<SaleDetailDTO> details;
}