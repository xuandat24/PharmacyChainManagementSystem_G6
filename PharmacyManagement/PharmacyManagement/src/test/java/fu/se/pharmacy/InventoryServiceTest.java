package fu.se.pharmacy;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class InventoryServiceTest {

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private SaleRepository saleRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetAvailableQuantity() {
        Integer branchId = 1;
        Integer medicineId = 1;

        Branch branch = new Branch();
        branch.setBranchId(branchId);

        Medicine medicine = new Medicine();
        medicine.setMedicineId(medicineId);

        List<Inventory> stock = new ArrayList<>();
        
        Inventory item1 = new Inventory();
        item1.setBranch(branch);
        item1.setMedicine(medicine);
        item1.setBatchNumber("LOT1");
        item1.setQuantity(50);
        item1.setExpiryDate(LocalDate.now().plusDays(10)); // Chưa hết hạn
        stock.add(item1);

        Inventory item2 = new Inventory();
        item2.setBranch(branch);
        item2.setMedicine(medicine);
        item2.setBatchNumber("LOT2");
        item2.setQuantity(30);
        item2.setExpiryDate(LocalDate.now().minusDays(5)); // Đã hết hạn
        stock.add(item2);

        when(inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineId(branchId, medicineId))
                .thenReturn(stock);

        int availableQty = inventoryService.getAvailableQuantity(branchId, medicineId);

        assertEquals(50, availableQty); // Chỉ lấy lô chưa hết hạn
        verify(inventoryRepository, times(1)).findByBranch_BranchIdAndMedicine_MedicineId(branchId, medicineId);
    }

    @Test
    public void testPostGoodsReceipt() {
        Integer receiptId = 10;

        Branch branch = new Branch();
        branch.setBranchId(1);

        Employee employee = new Employee();
        employee.setEmployeeId(2);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(receiptId);
        receipt.setBranch(branch);
        receipt.setReceivedBy(employee);
        receipt.setStatus("DRAFT");

        Medicine medicine = new Medicine();
        medicine.setMedicineId(1);
        medicine.setMedicineName("Panadol");

        List<GoodsReceiptDetail> details = new ArrayList<>();
        GoodsReceiptDetail d = new GoodsReceiptDetail();
        d.setMedicine(medicine);
        d.setBatchNumber("LOTX");
        d.setQuantityReceived(100);
        d.setExpiryDate(LocalDate.now().plusYears(1));
        d.setPurchasePrice(new BigDecimal("15000"));
        d.setGoodsReceipt(receipt);
        details.add(d);

        receipt.setDetails(details);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(1, 1, "LOTX"))
                .thenReturn(Optional.empty()); // Lô hàng chưa tồn tại

        inventoryService.postGoodsReceipt(receiptId);

        assertEquals("POSTED", receipt.getStatus());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
        verify(transactionRepository, times(1)).save(any(InventoryTransaction.class));
    }
}
