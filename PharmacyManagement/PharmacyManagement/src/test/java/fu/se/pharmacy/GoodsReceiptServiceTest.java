package fu.se.pharmacy;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.impl.GoodsReceiptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * GoodsReceiptServiceImpl dùng integer FK (không phải @ManyToOne).
 * - receipt.getPurchaseRequestId() → tìm PR qua purchaseRequestRepository
 * - grDetail.getMedicineId() / grDetail.getReceivedQuantity() / grDetail.getActualUnitPrice()
 * - prDetail.getMedicineId() / prDetail.getApprovedQuantity() / prDetail.getExpectedUnitPrice()
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsReceiptService - Unit Tests")
public class GoodsReceiptServiceTest {

    @InjectMocks private GoodsReceiptServiceImpl goodsReceiptService;

    @Mock private GoodsReceiptRepository goodsReceiptRepository;
    @Mock private GoodsReceiptDetailRepository goodsReceiptDetailRepository;
    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private PurchaseRequestDetailRepository purchaseRequestDetailRepository;
    @Mock private InventoryService inventoryService;

    // ======================== Setup ========================

    private GoodsReceipt buildReceipt(Integer receiptId, Integer prId, String status) {
        GoodsReceipt r = new GoodsReceipt();
        r.setReceiptId(receiptId);
        // FIX: integer FK thay vì setPurchaseRequest(pr)
        r.setPurchaseRequestId(prId);
        r.setBranchId(1);
        r.setSupplierId(2);
        r.setReceivedBy(3);
        r.setStatus(status);
        r.setTotalActualAmount(0);
        r.setHasVariance(false);
        return r;
    }

    private GoodsReceiptDetail buildGRDetail(Integer receiptId, Integer medicineId,
                                             Integer receivedQty, Integer actualPrice) {
        GoodsReceiptDetail d = new GoodsReceiptDetail();
        d.setGoodsReceiptId(receiptId);
        // FIX: setMedicineId(int) thay vì setMedicine(medicine)
        d.setMedicineId(medicineId);
        d.setBatchNumber("LOT-001");
        // FIX: setReceivedQuantity thay vì setQuantityReceived
        d.setReceivedQuantity(receivedQty);
        d.setAcceptedQuantity(receivedQty);
        d.setOrderedQuantity(receivedQty);
        // FIX: setActualUnitPrice(int) thay vì setPurchasePrice(BigDecimal)
        d.setActualUnitPrice(actualPrice);
        d.setInspectionResult("PASS");
        return d;
    }

    private PurchaseRequestDetail buildPRDetail(Integer prId, Integer medicineId,
                                                Integer approvedQty, Integer expectedPrice) {
        PurchaseRequestDetail d = new PurchaseRequestDetail();
        d.setPurchaseRequestId(prId);
        // FIX: setMedicineId(int) thay vì setMedicine(medicine)
        d.setMedicineId(medicineId);
        // FIX: setApprovedQuantity thay vì setQuantityApproved
        d.setApprovedQuantity(approvedQty);
        d.setRequestedQuantity(approvedQty);
        // FIX: setExpectedUnitPrice(int) thay vì setEstimatedPrice(BigDecimal)
        d.setExpectedUnitPrice(expectedPrice);
        return d;
    }

    // ======================== submitAndCheckVariance ========================

    @Test
    @DisplayName("Khop hoan toan - status POSTED, goi postGoodsReceipt")
    public void testSubmitAndCheckVariance_NoVariance() {
        Integer receiptId = 1;
        Integer prId = 10;

        GoodsReceipt receipt = buildReceipt(receiptId, prId, "DRAFT");
        GoodsReceiptDetail grDetail = buildGRDetail(receiptId, 100, 100, 18000);

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId(prId);
        pr.setStatus("APPROVED");

        PurchaseRequestDetail prDetail = buildPRDetail(prId, 100, 100, 18000);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));
        when(purchaseRequestDetailRepository.findByPurchaseRequestId(prId))
                .thenReturn(List.of(prDetail));
        when(goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId))
                .thenReturn(List.of(grDetail));
        when(goodsReceiptDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        goodsReceiptService.submitAndCheckVariance(receiptId);

        // Khop -> POSTED
        assertEquals("POSTED", receipt.getStatus());
        verify(inventoryService, times(1)).postGoodsReceipt(receiptId);
        verify(goodsReceiptRepository, atLeastOnce()).save(any(GoodsReceipt.class));
    }

    @Test
    @DisplayName("Vuot so luong duoc duyet - status PENDING_ADMIN_APPROVAL, khong goi kho")
    public void testSubmitAndCheckVariance_WithVariance_QuantityExceeded() {
        Integer receiptId = 2;
        Integer prId = 11;

        GoodsReceipt receipt = buildReceipt(receiptId, prId, "DRAFT");
        // Nhan 120 nhung PR chi duyet 100 → chenh lech
        GoodsReceiptDetail grDetail = buildGRDetail(receiptId, 100, 120, 18000);

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId(prId);
        pr.setStatus("APPROVED");

        PurchaseRequestDetail prDetail = buildPRDetail(prId, 100, 100, 18000);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));
        when(purchaseRequestDetailRepository.findByPurchaseRequestId(prId))
                .thenReturn(List.of(prDetail));
        when(goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId))
                .thenReturn(List.of(grDetail));
        when(goodsReceiptDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        goodsReceiptService.submitAndCheckVariance(receiptId);

        // Lech so luong -> PENDING_ADMIN_APPROVAL
        assertEquals("PENDING_ADMIN_APPROVAL", receipt.getStatus());
        verify(inventoryService, never()).postGoodsReceipt(receiptId);
        verify(goodsReceiptRepository, atLeastOnce()).save(any(GoodsReceipt.class));
    }

    @Test
    @DisplayName("Gia thuc mua cao hon gia du kien - status PENDING_ADMIN_APPROVAL")
    public void testSubmitAndCheckVariance_WithVariance_PriceExceeded() {
        Integer receiptId = 3;
        Integer prId = 12;

        GoodsReceipt receipt = buildReceipt(receiptId, prId, "DRAFT");
        // Gia thuc nhan 20000 > gia du kien 18000
        GoodsReceiptDetail grDetail = buildGRDetail(receiptId, 100, 100, 20000);

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId(prId);

        PurchaseRequestDetail prDetail = buildPRDetail(prId, 100, 100, 18000);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(purchaseRequestRepository.findById(prId)).thenReturn(Optional.of(pr));
        when(purchaseRequestDetailRepository.findByPurchaseRequestId(prId))
                .thenReturn(List.of(prDetail));
        when(goodsReceiptDetailRepository.findByGoodsReceiptId(receiptId))
                .thenReturn(List.of(grDetail));
        when(goodsReceiptDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        goodsReceiptService.submitAndCheckVariance(receiptId);

        assertEquals("PENDING_ADMIN_APPROVAL", receipt.getStatus());
        verify(inventoryService, never()).postGoodsReceipt(receiptId);
    }

    @Test
    @DisplayName("Phieu khong o DRAFT - nem exception")
    public void testSubmitAndCheckVariance_NotDraft_ThrowsException() {
        GoodsReceipt receipt = buildReceipt(1, 10, "POSTED");
        when(goodsReceiptRepository.findById(1)).thenReturn(Optional.of(receipt));

        assertThrows(IllegalStateException.class,
                () -> goodsReceiptService.submitAndCheckVariance(1));
        verify(inventoryService, never()).postGoodsReceipt(any());
    }

    @Test
    @DisplayName("approveVariance - duyet chenh lech, goi postGoodsReceipt")
    public void testApproveVariance_Success() {
        GoodsReceipt receipt = buildReceipt(1, 10, "PENDING_ADMIN_APPROVAL");
        when(goodsReceiptRepository.findById(1)).thenReturn(Optional.of(receipt));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        goodsReceiptService.approveVariance(1, 99, "Chap nhan chenh lech");

        assertEquals("POSTED", receipt.getStatus());
        assertEquals(99, receipt.getApprovedBy());
        verify(inventoryService).postGoodsReceipt(1);
    }

    @Test
    @DisplayName("rejectReceipt - tu choi, status REJECTED, khong goi kho")
    public void testRejectReceipt_Success() {
        GoodsReceipt receipt = buildReceipt(1, 10, "PENDING_ADMIN_APPROVAL");
        when(goodsReceiptRepository.findById(1)).thenReturn(Optional.of(receipt));
        when(goodsReceiptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        goodsReceiptService.rejectReceipt(1, 99, "Gia qua cao");

        assertEquals("REJECTED", receipt.getStatus());
        verify(inventoryService, never()).postGoodsReceipt(any());
    }
}