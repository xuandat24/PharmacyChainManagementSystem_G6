package fu.se.pharmacy;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.common.enums.ExpenseType;
import fu.se.pharmacy.common.enums.ExpenseVoucherStatus;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.ExpenseVoucherCreateRequest;
import fu.se.pharmacy.entity.ExpenseVoucher;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.repository.ExpenseVoucherRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.NotificationService;
import fu.se.pharmacy.service.PeriodClosingService;
import fu.se.pharmacy.service.SystemSettingService;
import fu.se.pharmacy.service.impl.ExpenseServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho ExpenseServiceImpl.
 * Business rules từ RDS:
 *   UC-31: Tạo phiếu chi DRAFT, submit → tự APPROVED (dưới hạn mức) hoặc SUBMITTED (chờ Admin)
 *   UC-32: Admin duyệt / từ chối SUBMITTED
 *   BR-EX-01: Amount phải > 0
 *   BR-EX-03: Vượt EXPENSE_APPROVAL_LIMIT hoặc loại OTHER → cần Admin
 *   BR-EX-04: Rejected không thể đổi trạng thái
 *   BR-EX-05: Status flow: DRAFT→SUBMITTED→APPROVED/REJECTED→PAID
 *   BR-PR-01: Không tạo / thanh toán khi kỳ đã khóa
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService - Unit Tests")
class ExpenseServiceTest {

    @Mock private ExpenseVoucherRepository expenseVoucherRepository;
    @Mock private PeriodClosingService periodClosingService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks private ExpenseServiceImpl expenseService;

    private ExpenseVoucher draftVoucher;

    @BeforeEach
    void setUp() {
        draftVoucher = new ExpenseVoucher();
        draftVoucher.setExpenseVoucherId(1);
        draftVoucher.setBranchId(1);
        draftVoucher.setCreatedBy(2);
        draftVoucher.setExpenseType(ExpenseType.ELECTRICITY);
        draftVoucher.setAmount(500_000);
        draftVoucher.setExpenseDate(LocalDate.of(2026, 6, 1));
        draftVoucher.setDescription("Tien dien thang 6");
        draftVoucher.setStatus(ExpenseVoucherStatus.DRAFT);
    }

    // ===========================================================
    // createExpense
    // ===========================================================

    @Nested
    @DisplayName("createExpense")
    class CreateExpense {

        @Test
        @DisplayName("Tao phieu chi - ky chua khoa - status DRAFT")
        void create_openPeriod_statusDraft() {
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(jdbcTemplate.queryForObject(contains("branches"), eq(Integer.class), eq(1))).thenReturn(1);
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(2))).thenReturn(1);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> {
                ExpenseVoucher e = inv.getArgument(0);
                e.setExpenseVoucherId(10);
                return e;
            });
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            ExpenseVoucherCreateRequest req = buildRequest(500_000, ExpenseType.ELECTRICITY);
            ExpenseVoucher result = expenseService.createExpense(req);

            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.DRAFT);
        }

        @Test
        @DisplayName("Tao phieu chi - ky da khoa - nem exception (BR-PR-01)")
        void create_lockedPeriod_throwsException() {
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(true);
            when(jdbcTemplate.queryForObject(contains("branches"), eq(Integer.class), eq(1))).thenReturn(1);
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(2))).thenReturn(1);

            assertThatThrownBy(() -> expenseService.createExpense(buildRequest(500_000, ExpenseType.ELECTRICITY)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("khóa");
        }
    }

    // ===========================================================
    // submitExpense
    // ===========================================================

    @Nested
    @DisplayName("submitExpense")
    class SubmitExpense {

        @Test
        @DisplayName("Submit - so tien duoi han muc va loai khong phai OTHER - tu dong APPROVED")
        void submit_belowLimitNonOther_autoApproved() {
            draftVoucher.setAmount(800_000);
            draftVoucher.setExpenseType(ExpenseType.ELECTRICITY);

            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(systemSettingService.getIntegerValue(eq(SettingKeys.EXPENSE_APPROVAL_LIMIT), anyInt()))
                    .thenReturn(1_000_000);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.submitExpense(1);

            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.APPROVED);
            verify(notificationService, never()).create(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit - so tien vuot han muc - SUBMITTED, thong bao Admin (BR-EX-03)")
        void submit_aboveLimit_statusSubmittedNotifiesAdmin() {
            draftVoucher.setAmount(2_000_000);
            draftVoucher.setExpenseType(ExpenseType.RENT);

            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(systemSettingService.getIntegerValue(eq(SettingKeys.EXPENSE_APPROVAL_LIMIT), anyInt()))
                    .thenReturn(1_000_000);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.submitExpense(1);

            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.SUBMITTED);
            verify(notificationService).create(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Submit - loai OTHER - luon can Admin duyet du so tien nho")
        void submit_expenseTypeOther_alwaysSubmitted() {
            draftVoucher.setAmount(100_000);
            draftVoucher.setExpenseType(ExpenseType.OTHER);

            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(systemSettingService.getIntegerValue(eq(SettingKeys.EXPENSE_APPROVAL_LIMIT), anyInt()))
                    .thenReturn(1_000_000);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());
            doNothing().when(notificationService).create(any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.submitExpense(1);

            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.SUBMITTED);
        }

        @Test
        @DisplayName("Submit - khong o DRAFT - nem exception (BR-EX-05)")
        void submit_notDraft_throwsException() {
            draftVoucher.setStatus(ExpenseVoucherStatus.SUBMITTED);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));

            assertThatThrownBy(() -> expenseService.submitExpense(1))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ===========================================================
    // approveExpense
    // ===========================================================

    @Nested
    @DisplayName("approveExpense")
    class ApproveExpense {

        @Test
        @DisplayName("Admin duyet SUBMITTED - status APPROVED")
        void approve_submitted_statusApproved() {
            draftVoucher.setStatus(ExpenseVoucherStatus.SUBMITTED);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(99))).thenReturn(1);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.approveExpense(1, approvalOf(99));
            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.APPROVED);
        }

        @Test
        @DisplayName("Admin duyet khong o SUBMITTED - nem exception")
        void approve_notSubmitted_throwsException() {
            draftVoucher.setStatus(ExpenseVoucherStatus.DRAFT);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(99))).thenReturn(1);

            assertThatThrownBy(() -> expenseService.approveExpense(1, approvalOf(99)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ===========================================================
    // rejectExpense
    // ===========================================================

    @Nested
    @DisplayName("rejectExpense")
    class RejectExpense {

        @Test
        @DisplayName("Admin tu choi SUBMITTED - status REJECTED")
        void reject_submitted_statusRejected() {
            draftVoucher.setStatus(ExpenseVoucherStatus.SUBMITTED);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(99))).thenReturn(1);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.rejectExpense(1, approvalOf(99));
            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.REJECTED);
        }
    }

    // ===========================================================
    // markPaid
    // ===========================================================

    @Nested
    @DisplayName("markPaid")
    class MarkPaid {

        @Test
        @DisplayName("Thanh toan phieu da APPROVED - status PAID")
        void markPaid_approved_statusPaid() {
            draftVoucher.setStatus(ExpenseVoucherStatus.APPROVED);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(3))).thenReturn(1);
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(false);
            when(expenseVoucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            ExpenseVoucher result = expenseService.markPaid(1, 3);
            assertThat(result.getStatus()).isEqualTo(ExpenseVoucherStatus.PAID);
        }

        @Test
        @DisplayName("Thanh toan phieu - ky da khoa - nem exception (BR-PR-01)")
        void markPaid_lockedPeriod_throwsException() {
            draftVoucher.setStatus(ExpenseVoucherStatus.APPROVED);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(3))).thenReturn(1);
            when(periodClosingService.isDateLocked(any(LocalDate.class))).thenReturn(true);

            assertThatThrownBy(() -> expenseService.markPaid(1, 3))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("khóa");
        }

        @Test
        @DisplayName("Thanh toan phieu - khong o APPROVED - nem exception (BR-EX-05)")
        void markPaid_notApproved_throwsException() {
            draftVoucher.setStatus(ExpenseVoucherStatus.DRAFT);
            when(expenseVoucherRepository.findById(1)).thenReturn(Optional.of(draftVoucher));
            when(jdbcTemplate.queryForObject(contains("app_users"), eq(Integer.class), eq(3))).thenReturn(1);

            assertThatThrownBy(() -> expenseService.markPaid(1, 3))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // Helpers
    private ExpenseVoucherCreateRequest buildRequest(int amount, ExpenseType type) {
        ExpenseVoucherCreateRequest req = new ExpenseVoucherCreateRequest();
        req.setBranchId(1);
        req.setCreatedBy(2);
        req.setExpenseType(type);
        req.setAmount(amount);
        req.setExpenseDate(LocalDate.of(2026, 6, 1));
        req.setDescription("Test expense");
        return req;
    }

    private ApprovalRequest approvalOf(int userId) {
        ApprovalRequest req = new ApprovalRequest();
        req.setApprovedBy(userId);
        req.setReason("Test reason");
        return req;
    }
}
