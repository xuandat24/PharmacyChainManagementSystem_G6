package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseRequestForm {

    @NotNull(message = "Vui long chon nha cung cap")
    private Integer supplierId;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull(message = "Thuoc khong duoc trong")
        private Integer medicineId;

        @NotNull(message = "So luong yeu cau khong duoc trong")
        @Min(value = 1, message = "So luong phai >= 1")
        private Integer quantityRequested;

        // FIX: BigDecimal → Integer (PurchaseRequestDetail.expectedUnitPrice là Integer)
        @Min(value = 0, message = "Gia du kien phai >= 0")
        private Integer estimatedPrice;
    }
}