package fu.se.pharmacy;

import fu.se.pharmacy.common.enums.NotificationType;
import fu.se.pharmacy.common.enums.StockTransferStatus;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.StockTransferCreateRequest;
import fu.se.pharmacy.dto.StockTransferReceiveRequest;
import fu.se.pharmacy.dto.StockTransferSendRequest;
import fu.se.pharmacy.entity.StockTransfer;
import fu.se.pharmacy.entity.StockTransferDetail;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.repository.StockTransferDetailRepository;
import fu.se.pharmacy.repository.StockTransferRepository;
import fu.se.pharmacy.service.*;
import fu.se.pharmacy.service.impl.StockTransferServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho StockTransferServiceImpl.
 * Business rules từ RDS (UC-28, UC-29, UC-30):
 *   BR-ST-01: Nhánh gửi và nhánh nhận không được trùng
 *   BR-ST-03: Nhánh gửi phải có đủ tồn kho
 *   BR-ST-04: Vượt TRANSFER_APPROVAL_LIMIT → PENDING_ADMIN_APPROVAL
 *   BR-ST-05: Chỉ SUBMITTED/PENDING_ADMIN_APPROVAL mới duyệt/từ chối được
 *   BR-ST-07: Chỉ APPROVED mới gửi hàng được
 *   BR-ST-10: Status flow: SUBMITTED→APPROVED→IN_TRANSIT→RECEIVED
 *   BR-PR-01: Không gửi/nhận khi kỳ đã khóa
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StockTransferService - Unit Tests")
class StockTransferServiceTest {

    @Mock private StockTransferRepository stockTransferRepository;
    @Mock private StockTransferDetailRepository stockTransferDetailRepository;
    @Mock private InventoryService inventoryService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private PeriodClosingService periodClosingService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks private StockTransferServiceImpl stockTransferService;

    private StockTransfer submittedTransfer;
    private StockTransferDetail detail;

    @BeforeEach
    void setUp() {
        detail = new StockTransferDetail();
        detail.setStockTransferDetailId(10);
        detail.setMedicineId(5);
        detail.setRequestedQuantity(50);

        submittedTransfer = new StockTransfer();
        submittedTransfer.setStockTransferId(1);
        submittedTransfer.setTransferCode("TRF-001");
        submittedTransfer.setFromBranchId(1);
        submittedTransfer.setToBranchId(2);
        submittedTransfer.setRequestedBy(3);
        submittedTransfer.setStatus(StockTransferStatus.SUBMITTED);
        submittedTransfer.setDetails(new ArrayList<>(List.of(detail)));
    }

    // ===========================================================
    // createTransfer
    // ===========================================================

    @Nested
    @DisplayName("createTransfer")
    class CreateTransfer {

        @Test
        @DisplayName("Tao phieu - nhanh gui trung nhanh nhan - nem exception (BR-ST-01)")
        void create_sameBranch_throwsException() {
            StockTransferCreateRequest req = buildRequest(1, 1, 3); // fromBranch = toBranch
            assertThatThrownBy(() -> stockTransferService.createTransfer(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("trùng");
        }

        @Test
        @DisplayName("Tao phieu - ton kho khong du - nem exception (BR-ST-03)")
        void create_insufficientStock_throwsException() {
            stubEntitiesExist();
            when(inventoryService.getAvailableQuantity(1, 5)).thenReturn(10); // can 50 nhung chi co 10
            when(jdbcTemplate.queryForObject(contains("sale_price"), eq(Integer.class), eq(5))).thenReturn(100_000);

            assertThatThrownBy(() -> stockTransferService.createTransfer(buildRequest(1, 2, 3)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("tồn kho");
        }

        @Test
        @DisplayName("Tao phieu - gia tri vuot han muc - status PENDING_ADMIN_APPROVAL (BR-ST-04)")
        void create_aboveLimit_statusPendingAdminApproval() {
            stubEntitiesExist();
            when(inventoryService.getAvailableQuantity(1, 5)).thenReturn(100);
            when(jdbcTemplate.queryForObject(contains("sale_price"), eq(Integer.class), eq(5))).thenReturn(200_000);
            // 50 * 200_000 = 10_000_000 > 3_000_000 (default limit)
            when(systemSettingService.getIntegerValue(anyString(), anyInt())).thenReturn(3_000_000);
            when(stockTransferRepository.save(any())).thenAnswer(inv -> {
                StockTransfer t = inv.getArgument(0);
                t.setStockTransferId(1);
                return t;
            });
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.createTransfer(buildRequest(1, 2, 3));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.PENDING_ADMIN_APPROVAL);
        }

        @Test
        @DisplayName("Tao phieu - gia tri duoi han muc - status SUBMITTED")
        void create_belowLimit_statusSubmitted() {
            stubEntitiesExist();
            when(inventoryService.getAvailableQuantity(1, 5)).thenReturn(100);
            when(jdbcTemplate.queryForObject(contains("sale_price"), eq(Integer.class), eq(5))).thenReturn(10_000);
            // 50 * 10_000 = 500_000 < 3_000_000
            when(systemSettingService.getIntegerValue(anyString(), anyInt())).thenReturn(3_000_000);
            when(stockTransferRepository.save(any())).thenAnswer(inv -> {
                StockTransfer t = inv.getArgument(0);
                t.setStockTransferId(1);
                return t;
            });
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.createTransfer(buildRequest(1, 2, 3));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.SUBMITTED);
        }
    }

    // ===========================================================
    // approveTransfer
    // ===========================================================

    @Nested
    @DisplayName("approveTransfer")
    class ApproveTransfer {

        @Test
        @DisplayName("Duyet phieu SUBMITTED - status APPROVED (BR-ST-05)")
        void approve_submitted_statusApproved() {
            stubUserExists(99);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.approveTransfer(1, approvalOf(99));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.APPROVED);
        }

        @Test
        @DisplayName("Duyet phieu PENDING_ADMIN_APPROVAL - status APPROVED")
        void approve_pendingAdminApproval_statusApproved() {
            submittedTransfer.setStatus(StockTransferStatus.PENDING_ADMIN_APPROVAL);
            stubUserExists(99);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.approveTransfer(1, approvalOf(99));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.APPROVED);
        }

        @Test
        @DisplayName("Duyet phieu da APPROVED - nem exception (BR-ST-05)")
        void approve_alreadyApproved_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.APPROVED);
            stubUserExists(99);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));

            assertThatThrownBy(() -> stockTransferService.approveTransfer(1, approvalOf(99)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("chờ duyệt");
        }
    }

    // ===========================================================
    // rejectTransfer
    // ===========================================================

    @Nested
    @DisplayName("rejectTransfer")
    class RejectTransfer {

        @Test
        @DisplayName("Tu choi phieu SUBMITTED - status REJECTED (BR-ST-06)")
        void reject_submitted_statusRejected() {
            stubUserExists(99);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.rejectTransfer(1, approvalOf(99));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.REJECTED);
        }
    }

    // ===========================================================
    // confirmSent
    // ===========================================================

    @Nested
    @DisplayName("confirmSent")
    class ConfirmSent {

        @Test
        @DisplayName("Gui hang - phieu APPROVED, ky chua khoa - status IN_TRANSIT (BR-ST-07)")
        void confirmSent_approved_statusInTransit() {
            submittedTransfer.setStatus(StockTransferStatus.APPROVED);
            stubUserExists(3);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(jdbcTemplate.queryForObject(contains("inventory_batches"), eq(Integer.class), eq(20))).thenReturn(1);
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryService).transferOut(1);
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.confirmSent(1, buildSendRequest(3, 10, 20));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.IN_TRANSIT);
            verify(inventoryService).transferOut(1);
        }

        @Test
        @DisplayName("Gui hang - ky da khoa - nem exception (BR-PR-01)")
        void confirmSent_lockedPeriod_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.APPROVED);
            stubUserExists(3);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(true);

            assertThatThrownBy(() -> stockTransferService.confirmSent(1, buildSendRequest(3, 10, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("khóa");
        }

        @Test
        @DisplayName("Gui hang - so luong vuot requested - nem exception")
        void confirmSent_sentQuantityExceedsRequested_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.APPROVED);
            detail.setRequestedQuantity(30);
            stubUserExists(3);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(jdbcTemplate.queryForObject(contains("inventory_batches"), eq(Integer.class), eq(20))).thenReturn(1);

            // sentQuantity = 50 > requestedQuantity = 30
            assertThatThrownBy(() -> stockTransferService.confirmSent(1, buildSendRequest(3, 50, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vượt");
        }

        @Test
        @DisplayName("Gui hang - phieu chua APPROVED - nem exception (BR-ST-07)")
        void confirmSent_notApproved_throwsException() {
            // status = SUBMITTED (chua duoc duyet)
            stubUserExists(3);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));

            assertThatThrownBy(() -> stockTransferService.confirmSent(1, buildSendRequest(3, 10, 20)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("duyệt");
        }
    }

    // ===========================================================
    // confirmReceived
    // ===========================================================

    @Nested
    @DisplayName("confirmReceived")
    class ConfirmReceived {

        @Test
        @DisplayName("Nhan hang - IN_TRANSIT, ky chua khoa - status RECEIVED")
        void confirmReceived_inTransit_statusReceived() {
            submittedTransfer.setStatus(StockTransferStatus.IN_TRANSIT);
            detail.setSentQuantity(30);
            stubUserExists(4);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryService).transferIn(1);
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.confirmReceived(1, buildReceiveRequest(4, 30));
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.RECEIVED);
            verify(inventoryService).transferIn(1);
        }

        @Test
        @DisplayName("Nhan hang co chenh lech - van RECEIVED, co thong bao")
        void confirmReceived_withVariance_receivedWithNotification() {
            submittedTransfer.setStatus(StockTransferStatus.IN_TRANSIT);
            detail.setSentQuantity(30);
            stubUserExists(4);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(inventoryService).transferIn(1);
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            // Nhan duoc 25, gui 30 → chenh lech
            stockTransferService.confirmReceived(1, buildReceiveRequest(4, 25));

            verify(notificationService).create(any(), eq(2), any(), any(), eq(NotificationType.TRANSFER));
        }

        @Test
        @DisplayName("Nhan hang - khong o IN_TRANSIT - nem exception")
        void confirmReceived_notInTransit_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.APPROVED);
            stubUserExists(4);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));

            assertThatThrownBy(() -> stockTransferService.confirmReceived(1, buildReceiveRequest(4, 30)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vận chuyển");
        }
    }

    // ===========================================================
    // cancelTransfer
    // ===========================================================

    @Nested
    @DisplayName("cancelTransfer")
    class CancelTransfer {

        @Test
        @DisplayName("Huy phieu SUBMITTED - thanh cong")
        void cancel_submitted_succeeds() {
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));
            when(stockTransferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            StockTransfer result = stockTransferService.cancelTransfer(1, 3, "Li do huy");
            assertThat(result.getStatus()).isEqualTo(StockTransferStatus.CANCELLED);
        }

        @Test
        @DisplayName("Huy phieu IN_TRANSIT - nem exception (BR-ST-09)")
        void cancel_inTransit_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.IN_TRANSIT);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));

            assertThatThrownBy(() -> stockTransferService.cancelTransfer(1, 3, "Test"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Huy phieu RECEIVED - nem exception")
        void cancel_received_throwsException() {
            submittedTransfer.setStatus(StockTransferStatus.RECEIVED);
            when(stockTransferRepository.findById(1)).thenReturn(Optional.of(submittedTransfer));

            assertThatThrownBy(() -> stockTransferService.cancelTransfer(1, 3, "Test"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private StockTransferCreateRequest buildRequest(int from, int to, int requestedBy) {
        StockTransferCreateRequest req = new StockTransferCreateRequest();
        req.setFromBranchId(from);
        req.setToBranchId(to);
        req.setRequestedBy(requestedBy);
        StockTransferCreateRequest.Item item = new StockTransferCreateRequest.Item();
        item.setMedicineId(5);
        item.setRequestedQuantity(50);
        req.setItems(List.of(item));
        return req;
    }

    private StockTransferSendRequest buildSendRequest(int sentBy, int sentQty, int batchId) {
        StockTransferSendRequest req = new StockTransferSendRequest();
        req.setSentBy(sentBy);
        StockTransferSendRequest.Item item = new StockTransferSendRequest.Item();
        item.setStockTransferDetailId(10);
        item.setSentQuantity(sentQty);
        item.setFromInventoryBatchId(batchId);
        req.setItems(List.of(item));
        return req;
    }

    private StockTransferReceiveRequest buildReceiveRequest(int receivedBy, int receivedQty) {
        StockTransferReceiveRequest req = new StockTransferReceiveRequest();
        req.setReceivedBy(receivedBy);
        StockTransferReceiveRequest.Item item = new StockTransferReceiveRequest.Item();
        item.setStockTransferDetailId(10);
        item.setReceivedQuantity(receivedQty);
        req.setItems(List.of(item));
        return req;
    }

    private ApprovalRequest approvalOf(int userId) {
        ApprovalRequest req = new ApprovalRequest();
        req.setApprovedBy(userId);
        req.setReason("Test");
        return req;
    }

    private void stubEntitiesExist() {
        when(jdbcTemplate.queryForObject(contains("branches"), eq(Integer.class), eq(1))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("branches"), eq(Integer.class), eq(2))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(3))).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("medicines"), eq(Integer.class), eq(5))).thenReturn(1);
    }

    private void stubUserExists(int userId) {
        when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(userId))).thenReturn(1);
    }
}
