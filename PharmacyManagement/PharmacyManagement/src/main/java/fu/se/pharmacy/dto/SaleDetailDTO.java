package fu.se.pharmacy.dto;

import lombok.Data;

/**
 * DTO một dòng thuốc trong hóa đơn, dùng để hiển thị.
 */
@Data
public class SaleDetailDTO {

    private Integer saleDetailId;
    private Integer saleId;
    private Integer medicineId;
    private String medicineName;   // join từ medicines
    private String medicineUnit;   // đơn vị tính
    private Integer inventoryBatchId;
    private Integer quantity;
    private Integer unitPrice;
    private Integer lineAmount;
}