package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.common.enums.ExpenseType;
import fu.se.pharmacy.common.enums.ExpenseVoucherStatus;
import fu.se.pharmacy.common.enums.NotificationType;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.ExpenseVoucherCreateRequest;
import fu.se.pharmacy.entity.ExpenseVoucher;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.exception.ResourceNotFoundException;
import fu.se.pharmacy.repository.ExpenseVoucherRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.SystemSettingService;
import fu.se.pharmacy.service.ExpenseService;
import fu.se.pharmacy.service.NotificationService;
import fu.se.pharmacy.service.PeriodClosingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseVoucherRepository expenseVoucherRepository;
    private final PeriodClosingService periodClosingService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;
    private final SystemSettingService systemSettingService;

    public ExpenseServiceImpl(ExpenseVoucherRepository expenseVoucherRepository,
                              PeriodClosingService periodClosingService,
                              AuditLogService auditLogService,
                              NotificationService notificationService,
                              JdbcTemplate jdbcTemplate,
                              SystemSettingService systemSettingService) {
        this.expenseVoucherRepository = expenseVoucherRepository;
        this.periodClosingService = periodClosingService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
        this.systemSettingService = systemSettingService;
    }

    @Override
    @Transactional
    public ExpenseVoucher createExpense(ExpenseVoucherCreateRequest request) {
        assertBranchExists(request.getBranchId());
        assertUserExists(request.getCreatedBy());
        if (periodClosingService.isDateLocked(request.getExpenseDate())) {
            throw new BusinessException("Không thể tạo phiếu chi trong kỳ đã khóa");
        }

        ExpenseVoucher expense = new ExpenseVoucher();
        expense.setExpenseCode(generateCode("EXP"));
        expense.setBranchId(request.getBranchId());
        expense.setCreatedBy(request.getCreatedBy());
        expense.setExpenseType(request.getExpenseType());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setDocumentNo(request.getDocumentNo());
        expense.setDescription(request.getDescription());
        expense.setStatus(ExpenseVoucherStatus.DRAFT);
        expense.setCreatedAt(LocalDateTime.now());

        ExpenseVoucher saved = expenseVoucherRepository.save(expense);
        auditLogService.log(request.getCreatedBy(), request.getBranchId(), "CREATE_EXPENSE", "ExpenseVoucher",
                saved.getExpenseVoucherId(), null, "DRAFT", request.getDescription());
        return saved;
    }

    @Override
    @Transactional
    public ExpenseVoucher submitExpense(Integer expenseId) {
        ExpenseVoucher expense = getById(expenseId);
        if (expense.getStatus() != ExpenseVoucherStatus.DRAFT) {
            throw new BusinessException("Chỉ được gửi duyệt phiếu chi ở trạng thái nháp");
        }

        int limit = getIntSetting(SettingKeys.EXPENSE_APPROVAL_LIMIT, 1000000);
        if (expense.getAmount() <= limit && expense.getExpenseType() != ExpenseType.OTHER) {
            expense.setStatus(ExpenseVoucherStatus.APPROVED);
            expense.setApprovedBy(expense.getCreatedBy());
            expense.setApprovedAt(LocalDateTime.now());
        } else {
            expense.setStatus(ExpenseVoucherStatus.SUBMITTED);
            notificationService.create(null, expense.getBranchId(), "Có phiếu chi cần duyệt",
                    "Phiếu " + expense.getExpenseCode() + " cần Admin duyệt", NotificationType.EXPENSE);
        }
        ExpenseVoucher saved = expenseVoucherRepository.save(expense);
        auditLogService.log(expense.getCreatedBy(), expense.getBranchId(), "SUBMIT_EXPENSE", "ExpenseVoucher",
                expenseId, "DRAFT", saved.getStatus().name(), null);
        return saved;
    }

    @Override
    @Transactional
    public ExpenseVoucher approveExpense(Integer expenseId, ApprovalRequest request) {
        ExpenseVoucher expense = getById(expenseId);
        assertUserExists(request.getApprovedBy());
        if (expense.getStatus() != ExpenseVoucherStatus.SUBMITTED) {
            throw new BusinessException("Chỉ được duyệt phiếu chi đang chờ duyệt");
        }
        expense.setStatus(ExpenseVoucherStatus.APPROVED);
        expense.setApprovedBy(request.getApprovedBy());
        expense.setApprovedAt(LocalDateTime.now());
        ExpenseVoucher saved = expenseVoucherRepository.save(expense);
        auditLogService.log(request.getApprovedBy(), expense.getBranchId(), "APPROVE_EXPENSE", "ExpenseVoucher",
                expenseId, "SUBMITTED", "APPROVED", request.getReason());
        return saved;
    }

    @Override
    @Transactional
    public ExpenseVoucher rejectExpense(Integer expenseId, ApprovalRequest request) {
        ExpenseVoucher expense = getById(expenseId);
        assertUserExists(request.getApprovedBy());
        if (expense.getStatus() != ExpenseVoucherStatus.SUBMITTED) {
            throw new BusinessException("Chỉ được từ chối phiếu chi đang chờ duyệt");
        }
        expense.setStatus(ExpenseVoucherStatus.REJECTED);
        expense.setApprovedBy(request.getApprovedBy());
        expense.setApprovedAt(LocalDateTime.now());
        ExpenseVoucher saved = expenseVoucherRepository.save(expense);
        auditLogService.log(request.getApprovedBy(), expense.getBranchId(), "REJECT_EXPENSE", "ExpenseVoucher",
                expenseId, "SUBMITTED", "REJECTED", request.getReason());
        return saved;
    }

    @Override
    @Transactional
    public ExpenseVoucher markPaid(Integer expenseId, Integer userId) {
        ExpenseVoucher expense = getById(expenseId);
        assertUserExists(userId);
        if (expense.getStatus() != ExpenseVoucherStatus.APPROVED) {
            throw new BusinessException("Chỉ được thanh toán phiếu chi đã duyệt");
        }
        if (periodClosingService.isDateLocked(expense.getExpenseDate())) {
            throw new BusinessException("Không thể thanh toán phiếu chi thuộc kỳ đã khóa");
        }
        expense.setStatus(ExpenseVoucherStatus.PAID);
        ExpenseVoucher saved = expenseVoucherRepository.save(expense);
        auditLogService.log(userId, expense.getBranchId(), "PAY_EXPENSE", "ExpenseVoucher",
                expenseId, "APPROVED", "PAID", null);
        return saved;
    }

    @Override
    public ExpenseVoucher getById(Integer expenseId) {
        return expenseVoucherRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu chi"));
    }

    @Override
    public List<ExpenseVoucher> getAll() {
        return expenseVoucherRepository.findAll();
    }

    @Override
    public List<ExpenseVoucher> getByBranch(Integer branchId) {
        return expenseVoucherRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
    }

    private int getIntSetting(String key, int defaultValue) {
        // FIX: dùng SystemSettingService thay vì JDBC riêng
        return systemSettingService.getIntegerValue(key, defaultValue);
    }

    private String generateCode(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void assertBranchExists(Integer branchId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM branches WHERE branch_id = ?", Integer.class, branchId);
        if (count == null || count == 0) throw new ResourceNotFoundException("Không tìm thấy chi nhánh ID: " + branchId);
    }

    private void assertUserExists(Integer userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM app_users WHERE user_id = ?", Integer.class, userId);
        if (count == null || count == 0) throw new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId);
    }
}
