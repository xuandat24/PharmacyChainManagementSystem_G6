package fu.se.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
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

        @NotBlank(message = "Số lô không được để trống")
        private String batchNumber;

        @NotNull(message = "Hạn sử dụng không được để trống")
        private LocalDate expiryDate;

        private LocalDate manufactureDate;

        @NotNull(message = "Số lượng thực nhận không được để trống")
        @Min(value = 0, message = "Số lượng thực nhận phải lớn hơn hoặc bằng 0")
        private Integer quantityReceived;

        @NotNull(message = "Giá thực mua không được để trống")
        @DecimalMin(value = "0.0", message = "Giá mua phải lớn hơn hoặc bằng 0")
        private BigDecimal purchasePrice;
    }
}
