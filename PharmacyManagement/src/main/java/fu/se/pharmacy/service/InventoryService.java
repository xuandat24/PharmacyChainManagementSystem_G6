package fu.se.pharmacy.service;

public interface InventoryService {

    int getAvailableQuantity(Integer branchId, Integer medicineId);

    void postGoodsReceipt(Integer receiptId);

    void deductForSale(Integer saleId);

    void restoreForCancelledSale(Integer saleId);

    void transferOut(Integer transferId);

    void transferIn(Integer transferId);

    void applyStockCount(Integer stockCountId);
}