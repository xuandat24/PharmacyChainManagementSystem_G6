package fu.se2033.pharmacy.service.impl;

import fu.se2033.pharmacy.service.InventoryService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("flow4-mock")
public class MockInventoryServiceImpl implements InventoryService {

    @Override
    public int getAvailableQuantity(Integer branchId, Integer medicineId) {
        return 9999;
    }

    @Override
    public void postGoodsReceipt(Integer receiptId) {
        // mock, chưa xử lý thật
    }

    @Override
    public void deductForSale(Integer saleId) {
        // mock, chưa xử lý thật
    }

    @Override
    public void restoreForCancelledSale(Integer saleId) {
        // mock, chưa xử lý thật
    }

    @Override
    public void transferOut(Integer transferId) {
        // mock, chưa xử lý thật
    }

    @Override
    public void transferIn(Integer transferId) {
        // mock, chưa xử lý thật
    }

    @Override
    public void applyStockCount(Integer stockCountId) {
        // mock, chưa xử lý thật
    }
}