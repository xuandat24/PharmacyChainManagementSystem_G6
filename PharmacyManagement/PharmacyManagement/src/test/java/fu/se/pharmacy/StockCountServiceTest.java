package fu.se.pharmacy;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.InventoryRepository;
import fu.se.pharmacy.repository.StockCountDetailRepository;
import fu.se.pharmacy.repository.StockCountRepository;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.impl.StockCountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class StockCountServiceTest {

    @InjectMocks
    private StockCountServiceImpl stockCountService;

    @Mock
    private StockCountRepository stockCountRepository;

    @Mock
    private StockCountDetailRepository detailRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryService inventoryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSubmitStockCount_UnderLimit() {
        Integer countId = 1;

        Branch branch = new Branch();
        branch.setBranchId(1);

        StockCount count = new StockCount();
        count.setStockCountId(countId);
        count.setBranch(branch);
        count.setStatus("DRAFT");

        Medicine medicine = new Medicine();
        medicine.setMedicineId(1);
        medicine.setUnitPrice(new BigDecimal("10000")); // giá bán 10k

        StockCountDetail d = new StockCountDetail();
        d.setMedicine(medicine);
        d.setBatchNumber("LOT1");
        d.setSystemQuantity(100);
        d.setActualQuantity(98); // Lệch giảm 2 hộp
        d.setDifference(-2);
        d.setStockCount(count);

        List<StockCountDetail> details = new ArrayList<>();
        details.add(d);
        count.setDetails(details);

        Inventory inv = new Inventory();
        inv.setPurchasePrice(new BigDecimal("8000")); // giá mua 8k -> tổng lệch = 2 * 8k = 16k <= 500k

        when(stockCountRepository.findById(countId)).thenReturn(Optional.of(count));
        when(inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(1, 1, "LOT1"))
                .thenReturn(Optional.of(inv));

        stockCountService.submitStockCount(countId);

        // Chênh lệch nhỏ -> COMPLETED ngay
        assertEquals("COMPLETED", count.getStatus());
        verify(inventoryService, times(1)).applyStockCount(countId);
        verify(stockCountRepository, times(1)).save(count);
    }

    @Test
    public void testSubmitStockCount_OverLimit() {
        Integer countId = 2;

        Branch branch = new Branch();
        branch.setBranchId(1);

        StockCount count = new StockCount();
        count.setStockCountId(countId);
        count.setBranch(branch);
        count.setStatus("DRAFT");

        Medicine medicine = new Medicine();
        medicine.setMedicineId(1);
        medicine.setUnitPrice(new BigDecimal("10000"));

        StockCountDetail d = new StockCountDetail();
        d.setMedicine(medicine);
        d.setBatchNumber("LOT1");
        d.setSystemQuantity(100);
        d.setActualQuantity(20); // Lệch giảm 80 hộp
        d.setDifference(-80);
        d.setStockCount(count);

        List<StockCountDetail> details = new ArrayList<>();
        details.add(d);
        count.setDetails(details);

        Inventory inv = new Inventory();
        inv.setPurchasePrice(new BigDecimal("8000")); // giá mua 8k -> tổng lệch = 80 * 8k = 640k > 500k limit

        when(stockCountRepository.findById(countId)).thenReturn(Optional.of(count));
        when(inventoryRepository.findByBranch_BranchIdAndMedicine_MedicineIdAndBatchNumber(1, 1, "LOT1"))
                .thenReturn(Optional.of(inv));

        stockCountService.submitStockCount(countId);

        // Chênh lệch lớn -> Chờ Admin phê duyệt
        assertEquals("PENDING_ADMIN_APPROVAL", count.getStatus());
        verify(inventoryService, never()).applyStockCount(countId);
        verify(stockCountRepository, times(1)).save(count);
    }
}
