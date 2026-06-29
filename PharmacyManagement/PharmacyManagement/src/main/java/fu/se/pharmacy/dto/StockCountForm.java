package fu.se.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class StockCountForm {

    @NotNull
    private Integer stockCountId;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        @NotNull
        private Integer detailId;

        @NotNull(message = "Số lượng thực tế không được để trống")
        @Min(value = 0, message = "Số lượng thực tế phải lớn hơn hoặc bằng 0")
        private Integer actualQuantity;

        private String reason;
    }
}
