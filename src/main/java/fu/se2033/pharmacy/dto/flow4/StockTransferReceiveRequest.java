package fu.se2033.pharmacy.dto.flow4;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public class StockTransferReceiveRequest {
    @NotNull(message = "Người nhận không được để trống")
    private Integer receivedBy;

    @Valid
    @NotEmpty(message = "Danh sách thuốc nhận không được để trống")
    private List<Item> items;

    public static class Item {
        @NotNull(message = "Chi tiết điều chuyển không được để trống")
        private Integer stockTransferDetailId;

        @NotNull(message = "Số lượng nhận không được để trống")
        @PositiveOrZero(message = "Số lượng nhận không được âm")
        private Integer receivedQuantity;

        public Integer getStockTransferDetailId() { return stockTransferDetailId; }
        public void setStockTransferDetailId(Integer stockTransferDetailId) { this.stockTransferDetailId = stockTransferDetailId; }
        public Integer getReceivedQuantity() { return receivedQuantity; }
        public void setReceivedQuantity(Integer receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    }

    public Integer getReceivedBy() { return receivedBy; }
    public void setReceivedBy(Integer receivedBy) { this.receivedBy = receivedBy; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
