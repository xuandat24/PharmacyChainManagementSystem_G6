package fu.se.pharmacy.service;

public interface InventoryService {

    /**
     * Lấy tổng số lượng tồn kho khả dụng của một loại thuốc tại một chi nhánh.
     * Tồn khả dụng = tổng số lượng của các lô chưa hết hạn và có quantity > 0.
     */
    int getAvailableQuantity(Integer branchId, Integer medicineId);

    /**
     * Xác nhận nhập kho từ Phiếu nhận hàng (Goods Receipt) đã được duyệt.
     * Tăng số lượng trong bảng Inventory và ghi nhận InventoryTransaction (RECEIPT).
     */
    void postGoodsReceipt(Integer receiptId);

    /**
     * Trừ tồn kho khi có đơn bán hàng thành công (Sale).
     * Trừ lần lượt từ lô có hạn dùng gần nhất (FEFO) tại chi nhánh đó.
     * Ghi nhận InventoryTransaction (SALE).
     */
    void deductForSale(Integer saleId);

    /**
     * Hoàn lại tồn kho khi đơn bán hàng bị hủy/hoàn tiền.
     * Cộng lại số lượng vào đúng các lô đã trừ trước đó.
     * Ghi nhận InventoryTransaction (SALE_RETURN).
     */
    void restoreForCancelledSale(Integer saleId);

    /**
     * Xuất kho điều chuyển sang chi nhánh khác.
     * Giảm số lượng tồn kho của chi nhánh xuất và ghi nhận InventoryTransaction (TRANSFER_OUT).
     */
    void transferOut(Integer transferId);

    /**
     * Nhận kho điều chuyển từ chi nhánh khác.
     * Tăng số lượng tồn kho của chi nhánh nhận và ghi nhận InventoryTransaction (TRANSFER_IN).
     */
    void transferIn(Integer transferId);

    /**
     * Áp dụng kết quả kiểm kê kho đã được duyệt.
     * Cập nhật số lượng tồn của các lô về số thực tế, ghi nhận chênh lệch tăng/giảm qua
     * InventoryTransaction (ADJUSTMENT_IN hoặc ADJUSTMENT_OUT).
     */
    void applyStockCount(Integer stockCountId);
}
