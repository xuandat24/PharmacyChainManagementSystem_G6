package fu.se.pharmacy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseRequestForm {

    @NotNull(message = "Vui lòng chọn nhà cung cấp")
    private Integer supplierId;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull(message = "Thuốc không được trống")
        private Integer medicineId;

        @NotNull(message = "Số lượng yêu cầu không được trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
        private Integer quantityRequested;

        @NotNull(message = "Giá dự kiến không được trống")
        @DecimalMin(value = "0.0", inclusive = false, message = "Giá dự kiến phải lớn hơn 0")
        private BigDecimal estimatedPrice;
    }
}
