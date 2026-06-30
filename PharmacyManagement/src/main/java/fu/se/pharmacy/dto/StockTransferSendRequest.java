package fu.se.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class StockTransferSendRequest {
    @NotNull(message = "Người gửi không được để trống")
    private Integer sentBy;

    @Valid
    @NotEmpty(message = "Danh sách thuốc gửi không được để trống")
    private List<Item> items;

    public static class Item {
        @NotNull(message = "Chi tiết điều chuyển không được để trống")
        private Integer stockTransferDetailId;

        @NotNull(message = "Lô xuất không được để trống")
        private Integer fromInventoryBatchId;

        @NotNull(message = "Số lượng gửi không được để trống")
        @Positive(message = "Số lượng gửi phải lớn hơn 0")
        private Integer sentQuantity;

        public Integer getStockTransferDetailId() { return stockTransferDetailId; }
        public void setStockTransferDetailId(Integer stockTransferDetailId) { this.stockTransferDetailId = stockTransferDetailId; }
        public Integer getFromInventoryBatchId() { return fromInventoryBatchId; }
        public void setFromInventoryBatchId(Integer fromInventoryBatchId) { this.fromInventoryBatchId = fromInventoryBatchId; }
        public Integer getSentQuantity() { return sentQuantity; }
        public void setSentQuantity(Integer sentQuantity) { this.sentQuantity = sentQuantity; }
    }

    public Integer getSentBy() { return sentBy; }
    public void setSentBy(Integer sentBy) { this.sentBy = sentBy; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
