package fu.se.pharmacy;

import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.EmployeeRepository;
import fu.se.pharmacy.repository.GoodsReceiptRepository;
import fu.se.pharmacy.repository.PurchaseRequestRepository;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.impl.GoodsReceiptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class GoodsReceiptServiceTest {

    @InjectMocks
    private GoodsReceiptServiceImpl goodsReceiptService;

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSubmitAndCheckVariance_NoVariance() {
        Integer receiptId = 1;

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestId(10);
        pr.setStatus("APPROVED");

        Medicine medicine = new Medicine();
        medicine.setMedicineId(100);
        medicine.setMedicineName("Panadol");

        // Chi tiết PR đề xuất 100 hộp giá 18000
        List<PurchaseRequestDetail> prDetails = new ArrayList<>();
        PurchaseRequestDetail prd = new PurchaseRequestDetail();
        prd.setMedicine(medicine);
        prd.setQuantityApproved(100);
        prd.setEstimatedPrice(new BigDecimal("18000"));
        prDetails.add(prd);
        pr.setDetails(prDetails);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(receiptId);
        receipt.setPurchaseRequest(pr);
        receipt.setStatus("DRAFT");

        // Chi tiết GR thực nhận 100 hộp giá 18000 (khớp hoàn toàn)
        List<GoodsReceiptDetail> grDetails = new ArrayList<>();
        GoodsReceiptDetail grd = new GoodsReceiptDetail();
        grd.setMedicine(medicine);
        grd.setQuantityReceived(100);
        grd.setPurchasePrice(new BigDecimal("18000"));
        grd.setGoodsReceipt(receipt);
        grDetails.add(grd);
        receipt.setDetails(grDetails);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));

        goodsReceiptService.submitAndCheckVariance(receiptId);

        // Khớp -> Trạng thái phải tự động chuyển thành POSTED
        assertEquals("POSTED", receipt.getStatus());
        // Khớp -> Gọi postGoodsReceipt để tăng tồn kho ngay lập tức
        verify(inventoryService, times(1)).postGoodsReceipt(receiptId);
        verify(goodsReceiptRepository, times(1)).save(receipt);
    }

    @Test
    public void testSubmitAndCheckVariance_WithVariance() {
        Integer receiptId = 2;

        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestId(11);
        pr.setStatus("APPROVED");

        Medicine medicine = new Medicine();
        medicine.setMedicineId(100);
        medicine.setMedicineName("Panadol");

        List<PurchaseRequestDetail> prDetails = new ArrayList<>();
        PurchaseRequestDetail prd = new PurchaseRequestDetail();
        prd.setMedicine(medicine);
        prd.setQuantityApproved(100);
        prd.setEstimatedPrice(new BigDecimal("18000"));
        prDetails.add(prd);
        pr.setDetails(prDetails);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptId(receiptId);
        receipt.setPurchaseRequest(pr);
        receipt.setStatus("DRAFT");

        // Chi tiết GR thực nhận 120 hộp (vượt 20 hộp so với duyệt)
        List<GoodsReceiptDetail> grDetails = new ArrayList<>();
        GoodsReceiptDetail grd = new GoodsReceiptDetail();
        grd.setMedicine(medicine);
        grd.setQuantityReceived(120);
        grd.setPurchasePrice(new BigDecimal("18000"));
        grd.setGoodsReceipt(receipt);
        grDetails.add(grd);
        receipt.setDetails(grDetails);

        when(goodsReceiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));

        goodsReceiptService.submitAndCheckVariance(receiptId);

        // Lệch -> Trạng thái phải chuyển thành PENDING_ADMIN_APPROVAL
        assertEquals("PENDING_ADMIN_APPROVAL", receipt.getStatus());
        // Lệch -> KHÔNG được gọi tăng kho trực tiếp
        verify(inventoryService, never()).postGoodsReceipt(receiptId);
        verify(goodsReceiptRepository, times(1)).save(receipt);
    }
}
