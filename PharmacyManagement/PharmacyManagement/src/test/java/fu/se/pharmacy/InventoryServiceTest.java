package fu.se.pharmacy;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InventoryServiceImpl dùng InventoryBatch (bảng inventory_batches), không dùng Inventory entity cũ.
 * - getAvailableQuantity() dùng inventoryBatchRepository.sumStock()
 * - postGoodsReceipt() dùng goodsReceiptDetailRepository.findByGoodsReceiptId()
 *   + inventoryBatchRepository để tìm/tạo lô
 * - GoodsReceipt: branchId(int), receivedBy(int) — không phải @ManyToOne
 * - GoodsReceiptDetail: medicineId(int), receivedQuantity(int), actualUnitPrice(int)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService - Unit Tests")
public class InventoryServiceTest {

    @InjectMocks private InventoryServiceImpl inventoryService;

    // FIX: InventoryBatchRepository thay vì InventoryRepository (Inventory entity đã không dùng)
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private SaleDetailRepository saleDetailRepository;
    @Mock private StockCountRepository stockCountRepository;
    @Mock private StockCountDetailRepository stockCountDetailRepository;
    @Mock private StockTransferRepository stockTransferRepository;

    // ======================== getAvailableQuantity ========================

    @Test
    @DisplayName("getAvailableQuantity - tra tong so luong tu sumStock")
    public void testGetAvailableQuantity_ReturnsSumStock() {
        // FIX: impl dùng sumStock(@Query) thay vì findBy + filter
        when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(80);

        int result = inventoryService.getAvailableQuantity(1, 1);

        assertEquals(80, result);
        verify(inventoryBatchRepository).sumStock(1, 1);
    }

    @Test
    @DisplayName("getAvailableQuantity - sumStock null tra ve 0")
    public void testGetAvailableQuantity_NullReturnsZero() {
        when(inventoryBatchRepository.sumStock(1, 99)).thenReturn(null);

        int result = inventoryService.getAvailableQuantity(1, 99);

        assertEquals(0, result);
    }

    // ======================== postGoodsReceipt ========================

    @Test
    @DisplayName("postGoodsReceipt - lo chua ton tai, tao lo moi, ghi transaction")
    public void testPostGoodsReceipt_NewBatch_CreatesAndSaves() {
        Integer receiptId = 10;

        // FIX: GoodsReceipt dùng integer FK, không phải @ManyToOne
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(receiptId);
        receipt.setBranchId(1);
        receipt.setSupplierId(2);
        receipt.setReceivedBy(3);  // FIX: Integer thay vì Employee
        receipt.setStatus("PENDING_ADMIN_APPROVAL"); // postGoodsReceipt chấp nhận mọi status != "POSTED"

        // FIX: GoodsReceiptDetail dùng integer FK
        GoodsReceiptDetail detail = new GoodsReceiptDetail();
        detail.setGoodsReceiptId(receiptId);
        detail.setMedicineId(1);  // FIX: medicineId(int) thay vì setMedicine(medicine)
        detail.setBatchNumber("LOTX");
        detail.setReceivedQuantity(100);  // FIX: receivedQuantity thay vì quantityReceived
        detail.setAcceptedQuantity(100);
        detail.setActualUnitPrice(15000);  // FIX: Integer thay vì BigDecimal
        detail.setExpiryDate(LocalDate.now().plusYears(1));
        detail.setInspectionResult("PASS");

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId))
                .thenReturn(List.of(detail));

        // Lo chua ton tai → tao moi
        when(inventoryBatchRepository.findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(1, 1, "AVAILABLE"))
                .thenReturn(List.of()); // rong

        InventoryBatch savedBatch = new InventoryBatch();
        savedBatch.setInventoryBatchId(100);
        savedBatch.setBranchId(1);
        savedBatch.setMedicineId(1);
        savedBatch.setQuantityOnHand(100);
        when(inventoryBatchRepository.save(any(InventoryBatch.class))).thenReturn(savedBatch);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> {
            GoodsReceipt r = inv.getArgument(0);
            assertEquals("POSTED", r.getStatus());
            return r;
        });

        inventoryService.postGoodsReceipt(receiptId);

        assertEquals("POSTED", receipt.getStatus());
        verify(inventoryBatchRepository).save(any(InventoryBatch.class));
        verify(transactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("postGoodsReceipt - da POSTED, skip")
    public void testPostGoodsReceipt_AlreadyPosted_Skip() {
        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(1);
        receipt.setStatus("POSTED");

        when(goodsReceiptRepository.findById(1)).thenReturn(Optional.of(receipt));

        inventoryService.postGoodsReceipt(1);

        // Da POSTED → khong lam gi
        verify(goodsReceiptDetailRepository, never()).findByGoodsReceiptId(any());
        verify(inventoryBatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("postGoodsReceipt - lo da ton tai, cong them so luong")
    public void testPostGoodsReceipt_ExistingBatch_AddsQuantity() {
        Integer receiptId = 20;

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(receiptId);
        receipt.setBranchId(1);
        receipt.setReceivedBy(3);
        receipt.setStatus("DRAFT");

        GoodsReceiptDetail detail = new GoodsReceiptDetail();
        detail.setGoodsReceiptId(receiptId);
        detail.setMedicineId(5);
        detail.setBatchNumber("LOTX");
        detail.setReceivedQuantity(50);
        detail.setAcceptedQuantity(50);
        detail.setActualUnitPrice(20000);
        detail.setExpiryDate(LocalDate.now().plusYears(2));
        detail.setInspectionResult("PASS");

        // Lo da ton tai voi batch_number "LOTX"
        InventoryBatch existing = new InventoryBatch();
        existing.setInventoryBatchId(10);
        existing.setBranchId(1);
        existing.setMedicineId(5);
        existing.setBatchNumber("LOTX");
        existing.setQuantityOnHand(200);
        existing.setStatus("AVAILABLE");

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId)).thenReturn(List.of(detail));
        when(inventoryBatchRepository.findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(1, 5, "AVAILABLE"))
                .thenReturn(List.of(existing));
        when(inventoryBatchRepository.save(any())).thenAnswer(inv -> {
            InventoryBatch b = inv.getArgument(0);
            // Co lo cu LOTX -> cong them 50
            assertEquals(250, b.getQuantityOnHand()); // 200 + 50
            return b;
        });
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.postGoodsReceipt(receiptId);

        verify(inventoryBatchRepository).save(argThat(b -> b.getQuantityOnHand() == 250));
    }

    // ======================== deductForSale ========================

    @Test
    @DisplayName("deductForSale - tru kho FIFO, set DISPOSED khi lo ve 0")
    public void testDeductForSale_Success_DeductsAndDisposesEmptyBatch() {
        Sale sale = new Sale();
        sale.setSaleId(1);
        sale.setBranchId(1);
        sale.setPharmacistId(5);

        SaleDetail detail = new SaleDetail();
        detail.setMedicineId(1);
        detail.setQuantity(10);

        InventoryBatch batch = new InventoryBatch();
        batch.setInventoryBatchId(1);
        batch.setQuantityOnHand(10); // vua du
        batch.setStatus("AVAILABLE");

        when(saleRepository.findById(1)).thenReturn(Optional.of(sale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(detail));
        when(inventoryBatchRepository.findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(1, 1, "AVAILABLE"))
                .thenReturn(List.of(batch));
        when(inventoryBatchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.deductForSale(1);

        verify(inventoryBatchRepository).save(argThat(b ->
                b.getQuantityOnHand() == 0 && "DISPOSED".equals(b.getStatus())));
        verify(transactionRepository).save(argThat(tx ->
                tx.getQuantityChange() == -10 && "SALE".equals(tx.getTransactionType())));
    }

    @Test
    @DisplayName("deductForSale - ton kho khong du - nem exception")
    public void testDeductForSale_InsufficientStock_ThrowsException() {
        Sale sale = new Sale();
        sale.setSaleId(1);
        sale.setBranchId(1);
        sale.setPharmacistId(5);

        SaleDetail detail = new SaleDetail();
        detail.setMedicineId(1);
        detail.setQuantity(100); // can 100

        InventoryBatch batch = new InventoryBatch();
        batch.setInventoryBatchId(1);
        batch.setQuantityOnHand(50); // chi co 50
        batch.setStatus("AVAILABLE");

        when(saleRepository.findById(1)).thenReturn(Optional.of(sale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(detail));
        when(inventoryBatchRepository.findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(1, 1, "AVAILABLE"))
                .thenReturn(List.of(batch));

        assertThrows(IllegalStateException.class, () -> inventoryService.deductForSale(1));
        verify(inventoryBatchRepository, never()).save(any());
    }
}