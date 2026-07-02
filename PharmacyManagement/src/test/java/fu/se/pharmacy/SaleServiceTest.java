package fu.se.pharmacy;

import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.PeriodClosingService;
import fu.se.pharmacy.service.PrescriptionService;
import fu.se.pharmacy.service.impl.SaleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService - Unit Tests")
class SaleServiceTest {

    @Mock private SaleRepository saleRepository;
    @Mock private SaleDetailRepository saleDetailRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private PrescriptionService prescriptionService;
    @Mock private MedicineRepository medicineRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    // FIX: SaleServiceImpl nay goi InventoryService.deductForSale() thay vi tu tru kho —
    // can mock 3 dependency moi (isDateLocked tra ve boolean mac dinh false, log() la void,
    // deductForSale() la void nen khong bat buoc phai stub, chi can khong null).
    @Mock private InventoryService inventoryService;
    @Mock private PeriodClosingService periodClosingService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private SaleServiceImpl saleService;

    private Sale draftSale;
    private Medicine otcMedicine;
    private Medicine rxMedicine;

    @BeforeEach
    void setUp() {
        draftSale = new Sale();
        draftSale.setSaleId(1);
        draftSale.setBranchId(1);
        draftSale.setPharmacistId(5);
        draftSale.setStatus("DRAFT");
        draftSale.setTotalAmount(0);
        draftSale.setDiscountAmount(0);
        draftSale.setFinalAmount(0);
        draftSale.setSaleDate(LocalDateTime.now());

        otcMedicine = new Medicine();
        otcMedicine.setMedicineId(1);
        otcMedicine.setMedicineName("Paracetamol 500mg");
        otcMedicine.setSalePrice(2000);
        otcMedicine.setUnit("vien");
        otcMedicine.setStatus("ACTIVE");
        otcMedicine.setRequiresPrescription(false);

        rxMedicine = new Medicine();
        rxMedicine.setMedicineId(5);
        rxMedicine.setMedicineName("Amoxicillin 500mg");
        rxMedicine.setSalePrice(5000);
        rxMedicine.setUnit("vien");
        rxMedicine.setStatus("ACTIVE");
        rxMedicine.setRequiresPrescription(true);
    }

    // ===========================================================
    // getOrCreateDraft
    // ===========================================================

    @Nested
    @DisplayName("getOrCreateDraft")
    class GetOrCreateDraft {

        @Test
        @DisplayName("Da co DRAFT - tra ve draft hien co, khong tao moi")
        void existingDraft_returnsExisting() {
            when(saleRepository.findByPharmacistIdAndStatus(5, "DRAFT"))
                    .thenReturn(List.of(draftSale));
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            SaleDTO result = saleService.getOrCreateDraft(5, 1);

            assertThat(result.getSaleId()).isEqualTo(1);
            verify(saleRepository, never()).save(any()); // khong tao moi
        }

        @Test
        @DisplayName("Chua co DRAFT - tao draft moi")
        void noDraft_createsNew() {
            Sale newSale = new Sale();
            newSale.setSaleId(10);
            newSale.setBranchId(1);
            newSale.setPharmacistId(5);
            newSale.setStatus("DRAFT");
            newSale.setTotalAmount(0);
            newSale.setDiscountAmount(0);
            newSale.setFinalAmount(0);

            when(saleRepository.findByPharmacistIdAndStatus(5, "DRAFT")).thenReturn(List.of());
            when(saleRepository.save(any(Sale.class))).thenReturn(newSale);
            when(saleDetailRepository.findBySaleId(10)).thenReturn(List.of());
            when(appUserRepository.findById(5)).thenReturn(Optional.empty());

            SaleDTO result = saleService.getOrCreateDraft(5, 1);

            assertThat(result.getSaleId()).isEqualTo(10);
            assertThat(result.getStatus()).isEqualTo("DRAFT");
            verify(saleRepository).save(any(Sale.class));
        }
    }

    // ===========================================================
    // addItem
    // ===========================================================

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("Them thuoc OTC - thanh cong")
        void addOtcItem_success_returnsNull() {
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(1)).thenReturn(Optional.of(otcMedicine));
            when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(100);
            when(saleDetailRepository.findBySaleIdAndMedicineId(1, 1)).thenReturn(Optional.empty());
            when(saleDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
            when(saleRepository.save(any())).thenReturn(draftSale);

            String error = saleService.addItem(1, 1, 10);

            assertThat(error).isNull(); // null = thanh cong
            verify(saleDetailRepository).save(any(SaleDetail.class));
        }

        @Test
        @DisplayName("Them thuoc - ton kho khong du tra ve thong bao loi")
        void addItem_insufficientStock_returnsError() {
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(1)).thenReturn(Optional.of(otcMedicine));
            when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(5); // chi co 5

            String error = saleService.addItem(1, 1, 10); // can 10

            assertThat(error).contains("Ton kho khong du").contains("5");
            verify(saleDetailRepository, never()).save(any());
        }

        @Test
        @DisplayName("Them thuoc - ton kho null (chua co lo nao) tra ve loi")
        void addItem_nullStock_returnsError() {
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(1)).thenReturn(Optional.of(otcMedicine));
            when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(null);

            String error = saleService.addItem(1, 1, 1);

            assertThat(error).contains("Ton kho khong du").contains("0");
        }

        @Test
        @DisplayName("Them thuoc - hoa don khong o DRAFT tra ve loi")
        void addItem_nonDraftSale_returnsError() {
            draftSale.setStatus("COMPLETED");
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));

            String error = saleService.addItem(1, 1, 5);

            assertThat(error).contains("DRAFT");
            verify(medicineRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Them thuoc Rx - chua chon khach hang tra ve loi")
        void addRxItem_noCustomer_returnsError() {
            draftSale.setCustomerId(null);
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(5)).thenReturn(Optional.of(rxMedicine));
            when(inventoryBatchRepository.sumStock(1, 5)).thenReturn(50);

            String error = saleService.addItem(1, 5, 5);

            assertThat(error).containsIgnoringCase("khach hang");
        }

        @Test
        @DisplayName("Them thuoc Rx - chua chon don thuoc tra ve loi")
        void addRxItem_noPrescription_returnsError() {
            draftSale.setCustomerId(2);
            draftSale.setPrescriptionId(null); // chua chon don thuoc
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(5)).thenReturn(Optional.of(rxMedicine));
            when(inventoryBatchRepository.sumStock(1, 5)).thenReturn(50);

            String error = saleService.addItem(1, 5, 5);

            assertThat(error).containsIgnoringCase("don thuoc");
        }

        @Test
        @DisplayName("Them thuoc Rx - don thuoc khong hop le tra ve loi")
        void addRxItem_invalidPrescription_returnsError() {
            draftSale.setCustomerId(2);
            draftSale.setPrescriptionId(1);
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(5)).thenReturn(Optional.of(rxMedicine));
            when(inventoryBatchRepository.sumStock(1, 5)).thenReturn(50);
            when(prescriptionService.isValidForSale(1, 5, 5)).thenReturn(false);

            String error = saleService.addItem(1, 5, 5);

            assertThat(error).containsIgnoringCase("don thuoc");
        }

        @Test
        @DisplayName("Them thuoc Rx - co don thuoc hop le - thanh cong")
        void addRxItem_validPrescription_success() {
            draftSale.setCustomerId(2);
            draftSale.setPrescriptionId(1);
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(5)).thenReturn(Optional.of(rxMedicine));
            when(inventoryBatchRepository.sumStock(1, 5)).thenReturn(50);
            when(prescriptionService.isValidForSale(1, 5, 21)).thenReturn(true);
            when(saleDetailRepository.findBySaleIdAndMedicineId(1, 5)).thenReturn(Optional.empty());
            when(saleDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
            when(saleRepository.save(any())).thenReturn(draftSale);

            String error = saleService.addItem(1, 5, 21);

            assertThat(error).isNull();
        }

        @Test
        @DisplayName("Them cung thuoc lan 2 - cong don so luong")
        void addSameItem_twice_accumulatesQuantity() {
            SaleDetail existing = new SaleDetail();
            existing.setSaleDetailId(1);
            existing.setSaleId(1);
            existing.setMedicineId(1);
            existing.setQuantity(5);
            existing.setUnitPrice(2000);
            existing.setLineAmount(10000);

            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(1)).thenReturn(Optional.of(otcMedicine));
            when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(100);
            when(saleDetailRepository.findBySaleIdAndMedicineId(1, 1)).thenReturn(Optional.of(existing));
            when(saleDetailRepository.save(any())).thenAnswer(inv -> {
                SaleDetail d = inv.getArgument(0);
                assertThat(d.getQuantity()).isEqualTo(10); // 5 + 5
                assertThat(d.getLineAmount()).isEqualTo(20000);
                return d;
            });
            when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(existing));
            when(saleRepository.save(any())).thenReturn(draftSale);

            String error = saleService.addItem(1, 1, 5);
            assertThat(error).isNull();
        }

        @Test
        @DisplayName("Them thuoc ngung kinh doanh tra ve loi")
        void addItem_discontinuedMedicine_returnsError() {
            otcMedicine.setStatus("INACTIVE");
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(medicineRepository.findById(1)).thenReturn(Optional.of(otcMedicine));
            when(inventoryBatchRepository.sumStock(1, 1)).thenReturn(100);

            String error = saleService.addItem(1, 1, 5);
            assertThat(error).containsIgnoringCase("ngung");
        }
    }

    // ===========================================================
    // cancelDraft
    // ===========================================================

    @Nested
    @DisplayName("cancelDraft")
    class CancelDraft {

        @Test
        @DisplayName("Huy DRAFT thanh cong - status doi sang VOIDED")
        void cancelDraft_success() {
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(saleRepository.save(any())).thenAnswer(inv -> {
                Sale s = inv.getArgument(0);
                assertThat(s.getStatus()).isEqualTo("VOIDED");
                return s;
            });

            saleService.cancelDraft(1);
            verify(saleRepository).save(any(Sale.class));
        }

        @Test
        @DisplayName("Huy don da COMPLETED - nem exception")
        void cancelDraft_completedSale_throwsException() {
            draftSale.setStatus("COMPLETED");
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));

            assertThatThrownBy(() -> saleService.cancelDraft(1))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DRAFT");
        }
    }

    // ===========================================================
    // completeSale
    // ===========================================================

    @Nested
    @DisplayName("completeSale")
    class CompleteSale {

        @Test
        @DisplayName("completeSale - status doi sang COMPLETED, goi InventoryService.deductForSale")
        void completeSale_statusCompleted_delegatesToInventoryService() {
            // FIX: SaleServiceImpl khong con tu tru kho (deductInventoryFifo) — logic tru kho
            // FIFO + ghi InventoryTransaction nay thuoc ve InventoryServiceImpl (da co test
            // rieng trong InventoryServiceTest). O day chi kiem tra SaleServiceImpl:
            // 1) doi status sang COMPLETED, 2) uy quyen dung cho InventoryService.deductForSale().
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(saleRepository.save(any())).thenAnswer(inv -> {
                Sale s = inv.getArgument(0);
                assertThat(s.getStatus()).isEqualTo("COMPLETED");
                return s;
            });

            saleService.completeSale(1);

            verify(saleRepository).save(argThat(s -> "COMPLETED".equals(s.getStatus())));
            verify(inventoryService).deductForSale(1);
            verify(inventoryBatchRepository, never()).save(any());
        }

        @Test
        @DisplayName("completeSale - ky ke toan da khoa - nem exception, khong tru kho")
        void completeSale_periodLocked_throwsException() {
            when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
            when(periodClosingService.isDateLocked(any())).thenReturn(true);

            assertThatThrownBy(() -> saleService.completeSale(1))
                    .isInstanceOf(RuntimeException.class);

            verify(inventoryService, never()).deductForSale(any());
            verify(saleRepository, never()).save(any());
        }
    }

    // ===========================================================
    // setCustomer / setPrescription
    // ===========================================================

    @Test
    @DisplayName("setCustomer - gan customerId vao sale")
    void setCustomer_setsCustomerId() {
        when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
        when(saleRepository.save(any())).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            assertThat(s.getCustomerId()).isEqualTo(2);
            return s;
        });

        saleService.setCustomer(1, 2);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    @DisplayName("setPrescription - gan prescriptionId vao sale")
    void setPrescription_setsPrescriptionId() {
        when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
        when(saleRepository.save(any())).thenAnswer(inv -> {
            Sale s = inv.getArgument(0);
            assertThat(s.getPrescriptionId()).isEqualTo(1);
            return s;
        });

        saleService.setPrescription(1, 1);
        verify(saleRepository).save(any(Sale.class));
    }

    // ===========================================================
    // updateItemQuantity
    // ===========================================================

    @Test
    @DisplayName("updateItemQuantity - quantity > 0 - cap nhat so luong")
    void updateItemQuantity_positive_updates() {
        SaleDetail detail = new SaleDetail();
        detail.setSaleDetailId(1);
        detail.setSaleId(1);
        detail.setQuantity(5);
        detail.setUnitPrice(2000);
        detail.setLineAmount(10000);

        when(saleDetailRepository.findById(1)).thenReturn(Optional.of(detail));
        when(saleDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of(detail));
        when(saleRepository.save(any())).thenReturn(draftSale);

        saleService.updateItemQuantity(1, 8);

        verify(saleDetailRepository).save(argThat(d -> d.getQuantity() == 8 && d.getLineAmount() == 16000));
    }

    @Test
    @DisplayName("updateItemQuantity - quantity <= 0 - xoa dong hang")
    void updateItemQuantity_zeroOrNegative_deletesDetail() {
        SaleDetail detail = new SaleDetail();
        detail.setSaleDetailId(1);
        detail.setSaleId(1);
        detail.setQuantity(5);
        detail.setUnitPrice(2000);

        when(saleDetailRepository.findById(1)).thenReturn(Optional.of(detail));
        doNothing().when(saleDetailRepository).deleteById(1);
        when(saleRepository.findById(1)).thenReturn(Optional.of(draftSale));
        when(saleDetailRepository.findBySaleId(1)).thenReturn(List.of());
        when(saleRepository.save(any())).thenReturn(draftSale);

        saleService.updateItemQuantity(1, 0);

        verify(saleDetailRepository).deleteById(1);
        verify(saleDetailRepository, never()).save(any());
    }
}
