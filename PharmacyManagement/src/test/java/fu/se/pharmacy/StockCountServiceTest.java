package fu.se.pharmacy;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.PeriodClosingService;
import fu.se.pharmacy.service.SystemSettingService;
import fu.se.pharmacy.service.impl.StockCountServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho StockCountServiceImpl.
 * Business rules từ RDS:
 *   UC-17: Tạo / submit phiếu kiểm kê
 *   UC-18: Admin duyệt khi variance vượt STOCK_VARIANCE_LIMIT
 *   BR-20: Chênh lệch vượt hạn mức → cần Admin duyệt trước khi điều chỉnh kho
 *   BR-PR-01: Không tạo / duyệt khi kỳ kế toán đã bị khóa
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockCountService - Unit Tests")
class StockCountServiceTest {

    @Mock private StockCountRepository stockCountRepository;
    @Mock private StockCountDetailRepository detailRepository;
    @Mock private InventoryBatchRepository inventoryBatchRepository;
    @Mock private InventoryService inventoryService;
    @Mock private SystemSettingService systemSettingService;
    @Mock private AuditLogService auditLogService;
    @Mock private PeriodClosingService periodClosingService;

    @InjectMocks private StockCountServiceImpl stockCountService;

    private StockCount draftCount;
    private StockCountDetail detail;

    @BeforeEach
    void setUp() {
        draftCount = new StockCount();
        draftCount.setStockCountId(1);
        draftCount.setBranchId(1);
        draftCount.setCreatedBy(2);
        draftCount.setStatus("DRAFT");

        detail = new StockCountDetail();
        detail.setStockCountDetailId(10);
        detail.setStockCountId(1);
        detail.setInventoryBatchId(5);
        detail.setSystemQuantity(100);
        detail.setActualQuantity(100); // khong chenh lech mac dinh
    }

    // ===========================================================
    // submitStockCount
    // ===========================================================

    @Nested
    @DisplayName("submitStockCount")
    class SubmitStockCount {

        @Test
        @DisplayName("Submit - variance thap - status POSTED, ap dung kho ngay")
        void submit_lowVariance_statusPosted() {
            detail.setActualQuantity(98); // chenh lech 2 don vi
            // unitCost = 50_000 → variance value = 2 * 50_000 = 100_000 < 500_000

            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(detailRepository.findByStockCountId(1)).thenReturn(List.of(detail));
            when(inventoryBatchRepository.findById(5)).thenReturn(Optional.of(batchWithCost(50_000)));
            when(systemSettingService.getMoneyLimit(eq(SettingKeys.STOCK_VARIANCE_LIMIT), any()))
                    .thenReturn(BigDecimal.valueOf(500_000));
            when(stockCountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryService).applyStockCount(1);

            stockCountService.submitStockCount(1);

            verify(stockCountRepository).save(argThat(c -> "POSTED".equals(c.getStatus())));
            verify(inventoryService).applyStockCount(1);
        }

        @Test
        @DisplayName("Submit - variance cao - status SUBMITTED, can Admin duyet (BR-20)")
        void submit_highVariance_statusSubmitted_needsAdminApproval() {
            detail.setActualQuantity(80); // chenh lech 20 don vi
            // unitCost = 50_000 → variance value = 20 * 50_000 = 1_000_000 > 500_000

            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(detailRepository.findByStockCountId(1)).thenReturn(List.of(detail));
            when(inventoryBatchRepository.findById(5)).thenReturn(Optional.of(batchWithCost(50_000)));
            when(systemSettingService.getMoneyLimit(eq(SettingKeys.STOCK_VARIANCE_LIMIT), any()))
                    .thenReturn(BigDecimal.valueOf(500_000));
            when(stockCountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockCountService.submitStockCount(1);

            verify(stockCountRepository).save(argThat(c -> "SUBMITTED".equals(c.getStatus())));
            verify(inventoryService, never()).applyStockCount(any());
        }

        @Test
        @DisplayName("Submit - ky da khoa - nem exception (BR-PR-01)")
        void submit_lockedPeriod_throwsException() {
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(true);

            assertThatThrownBy(() -> stockCountService.submitStockCount(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("khóa");

            verify(stockCountRepository, never()).save(any());
        }

        @Test
        @DisplayName("Submit - khong o DRAFT - nem exception")
        void submit_notDraft_throwsException() {
            draftCount.setStatus("SUBMITTED");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));

            assertThatThrownBy(() -> stockCountService.submitStockCount(1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===========================================================
    // approveStockCount
    // ===========================================================

    @Nested
    @DisplayName("approveStockCount")
    class ApproveStockCount {

        @Test
        @DisplayName("Admin duyet - status APPROVED, ap dung kho")
        void approve_submitted_statusApproved() {
            draftCount.setStatus("SUBMITTED");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(stockCountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryService).applyStockCount(1);
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            stockCountService.approveStockCount(1, 99, "OK");

            verify(stockCountRepository).save(argThat(c -> "APPROVED".equals(c.getStatus())));
            verify(inventoryService).applyStockCount(1);
        }

        @Test
        @DisplayName("Admin duyet - ky da khoa - nem exception")
        void approve_lockedPeriod_throwsException() {
            draftCount.setStatus("SUBMITTED");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(true);

            assertThatThrownBy(() -> stockCountService.approveStockCount(1, 99, "Test"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("khóa");

            verify(inventoryService, never()).applyStockCount(any());
        }

        @Test
        @DisplayName("Admin duyet - khong o SUBMITTED - nem exception")
        void approve_notSubmitted_throwsException() {
            draftCount.setStatus("DRAFT");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));

            assertThatThrownBy(() -> stockCountService.approveStockCount(1, 99, "Test"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===========================================================
    // rejectStockCount
    // ===========================================================

    @Nested
    @DisplayName("rejectStockCount")
    class RejectStockCount {

        @Test
        @DisplayName("Bac bo - status CANCELLED")
        void reject_submitted_statusCancelled() {
            draftCount.setStatus("SUBMITTED");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));
            when(stockCountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            stockCountService.rejectStockCount(1, 99, "Ly do bac bo");

            verify(stockCountRepository).save(argThat(c -> "CANCELLED".equals(c.getStatus())));
            verify(inventoryService, never()).applyStockCount(any());
        }

        @Test
        @DisplayName("Bac bo - khong o SUBMITTED - nem exception")
        void reject_notSubmitted_throwsException() {
            draftCount.setStatus("POSTED");
            when(stockCountRepository.findById(1)).thenReturn(Optional.of(draftCount));

            assertThatThrownBy(() -> stockCountService.rejectStockCount(1, 99, "Test"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // Helper
    private InventoryBatch batchWithCost(int unitCost) {
        InventoryBatch b = new InventoryBatch();
        b.setInventoryBatchId(5);
        b.setUnitCost(unitCost);
        return b;
    }
}
