package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class GoodsReceiptForm {

    @NotNull
    private Integer purchaseRequestId;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull
        private Integer medicineId;

        // batchNumber không bắt buộc - controller tự gen nếu null
        private String batchNumber;

        @NotNull(message = "Han su dung khong duoc de trong")
        private LocalDate expiryDate;

        private LocalDate manufactureDate;

        @NotNull(message = "So luong thuc nhan khong duoc de trong")
        @Min(value = 0, message = "So luong thuc nhan phai >= 0")
        private Integer quantityReceived;

        // FIX: BigDecimal → Integer (entity GoodsReceiptDetail.actualUnitPrice là Integer)
        @Min(value = 0, message = "Gia mua phai >= 0")
        private Integer purchasePrice;
    }
}