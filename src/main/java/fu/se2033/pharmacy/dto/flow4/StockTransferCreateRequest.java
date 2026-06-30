package fu.se2033.pharmacy.dto.flow4;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class StockTransferCreateRequest {
    @NotNull(message = "Chi nhánh gửi không được để trống")
    private Integer fromBranchId;

    @NotNull(message = "Chi nhánh nhận không được để trống")
    private Integer toBranchId;

    @NotNull(message = "Người tạo yêu cầu không được để trống")
    private Integer requestedBy;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;

    @Valid
    @NotEmpty(message = "Phiếu điều chuyển phải có ít nhất một thuốc")
    private List<Item> items;

    public static class Item {
        @NotNull(message = "Thuốc không được để trống")
        private Integer medicineId;

        @NotNull(message = "Số lượng yêu cầu không được để trống")
        @Positive(message = "Số lượng yêu cầu phải lớn hơn 0")
        private Integer requestedQuantity;

        public Integer getMedicineId() { return medicineId; }
        public void setMedicineId(Integer medicineId) { this.medicineId = medicineId; }
        public Integer getRequestedQuantity() { return requestedQuantity; }
        public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    }

    public Integer getFromBranchId() { return fromBranchId; }
    public void setFromBranchId(Integer fromBranchId) { this.fromBranchId = fromBranchId; }
    public Integer getToBranchId() { return toBranchId; }
    public void setToBranchId(Integer toBranchId) { this.toBranchId = toBranchId; }
    public Integer getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Integer requestedBy) { this.requestedBy = requestedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
