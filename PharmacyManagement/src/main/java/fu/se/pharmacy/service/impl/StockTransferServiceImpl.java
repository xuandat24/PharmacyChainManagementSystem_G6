package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.common.enums.NotificationType;
import fu.se.pharmacy.common.enums.StockTransferStatus;
import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.StockTransferCreateRequest;
import fu.se.pharmacy.dto.StockTransferReceiveRequest;
import fu.se.pharmacy.dto.StockTransferSendRequest;
import fu.se.pharmacy.entity.StockTransfer;
import fu.se.pharmacy.entity.StockTransferDetail;
import fu.se.pharmacy.exception.BusinessException;
import fu.se.pharmacy.exception.ResourceNotFoundException;
import fu.se.pharmacy.repository.StockTransferDetailRepository;
import fu.se.pharmacy.repository.StockTransferRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.NotificationService;
import fu.se.pharmacy.service.StockTransferService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockTransferServiceImpl implements StockTransferService {
    private final StockTransferRepository stockTransferRepository;
    private final StockTransferDetailRepository stockTransferDetailRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbcTemplate;

    public StockTransferServiceImpl(StockTransferRepository stockTransferRepository,
                                    StockTransferDetailRepository stockTransferDetailRepository,
                                    InventoryService inventoryService,
                                    AuditLogService auditLogService,
                                    NotificationService notificationService,
                                    JdbcTemplate jdbcTemplate) {
        this.stockTransferRepository = stockTransferRepository;
        this.stockTransferDetailRepository = stockTransferDetailRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public StockTransfer createTransfer(StockTransferCreateRequest request) {
        if (request.getFromBranchId().equals(request.getToBranchId())) {
            throw new BusinessException("Chi nhánh gửi và chi nhánh nhận không được trùng nhau");
        }
        assertBranchExists(request.getFromBranchId());
        assertBranchExists(request.getToBranchId());
        assertUserExists(request.getRequestedBy());

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferCode(generateCode("TRF"));
        transfer.setFromBranchId(request.getFromBranchId());
        transfer.setToBranchId(request.getToBranchId());
        transfer.setRequestedBy(request.getRequestedBy());
        transfer.setRequestedAt(LocalDateTime.now());
        transfer.setNote(request.getNote());
        transfer.setTotalValueAmount(calculateTransferValue(request));

        int limit = getIntSetting(SettingKeys.TRANSFER_APPROVAL_LIMIT, 3000000);
        if (transfer.getTotalValueAmount() > limit) {
            transfer.setStatus(StockTransferStatus.PENDING_ADMIN_APPROVAL);
        } else {
            transfer.setStatus(StockTransferStatus.SUBMITTED);
        }

        request.getItems().forEach(item -> {
            assertMedicineExists(item.getMedicineId());
            int availableQty = inventoryService.getAvailableQuantity(request.getFromBranchId(), item.getMedicineId());
            if (availableQty < item.getRequestedQuantity()) {
                throw new BusinessException("Không đủ tồn kho để điều chuyển thuốc ID: " + item.getMedicineId());
            }
            StockTransferDetail detail = new StockTransferDetail();
            detail.setMedicineId(item.getMedicineId());
            detail.setRequestedQuantity(item.getRequestedQuantity());
            transfer.addDetail(detail);
        });

        StockTransfer saved = stockTransferRepository.save(transfer);
        auditLogService.log(request.getRequestedBy(), request.getFromBranchId(), "CREATE_TRANSFER", "StockTransfer",
                saved.getStockTransferId(), null, saved.getStatus().name(), request.getNote());
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer approveTransfer(Integer transferId, ApprovalRequest request) {
        StockTransfer transfer = getById(transferId);
        assertUserExists(request.getApprovedBy());
        if (transfer.getStatus() != StockTransferStatus.SUBMITTED
                && transfer.getStatus() != StockTransferStatus.PENDING_ADMIN_APPROVAL) {
            throw new BusinessException("Chỉ được duyệt phiếu đang chờ duyệt");
        }
        StockTransferStatus oldStatus = transfer.getStatus();
        transfer.setStatus(StockTransferStatus.APPROVED);
        transfer.setApprovedBy(request.getApprovedBy());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditLogService.log(request.getApprovedBy(), transfer.getFromBranchId(), "APPROVE_TRANSFER", "StockTransfer",
                transferId, oldStatus.name(), saved.getStatus().name(), request.getReason());
        notificationService.create(null, transfer.getFromBranchId(), "Phiếu điều chuyển đã được duyệt",
                "Phiếu " + transfer.getTransferCode() + " đã sẵn sàng để gửi hàng", NotificationType.TRANSFER);
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer rejectTransfer(Integer transferId, ApprovalRequest request) {
        StockTransfer transfer = getById(transferId);
        assertUserExists(request.getApprovedBy());
        if (transfer.getStatus() != StockTransferStatus.SUBMITTED
                && transfer.getStatus() != StockTransferStatus.PENDING_ADMIN_APPROVAL) {
            throw new BusinessException("Chỉ được từ chối phiếu đang chờ duyệt");
        }
        StockTransferStatus oldStatus = transfer.getStatus();
        transfer.setStatus(StockTransferStatus.REJECTED);
        transfer.setApprovedBy(request.getApprovedBy());
        transfer.setApprovedAt(LocalDateTime.now());
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditLogService.log(request.getApprovedBy(), transfer.getFromBranchId(), "REJECT_TRANSFER", "StockTransfer",
                transferId, oldStatus.name(), saved.getStatus().name(), request.getReason());
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer confirmSent(Integer transferId, StockTransferSendRequest request) {
        StockTransfer transfer = getById(transferId);
        assertUserExists(request.getSentBy());
        if (transfer.getStatus() != StockTransferStatus.APPROVED) {
            throw new BusinessException("Chỉ được gửi hàng khi phiếu đã được duyệt");
        }

        Map<Integer, StockTransferSendRequest.Item> itemMap = request.getItems().stream()
                .collect(Collectors.toMap(StockTransferSendRequest.Item::getStockTransferDetailId, i -> i));

        for (StockTransferDetail detail : transfer.getDetails()) {
            StockTransferSendRequest.Item item = itemMap.get(detail.getStockTransferDetailId());
            if (item == null) {
                throw new BusinessException("Thiếu thông tin gửi cho thuốc ID: " + detail.getMedicineId());
            }
            if (item.getSentQuantity() > detail.getRequestedQuantity()) {
                throw new BusinessException("Số lượng gửi không được vượt số lượng yêu cầu");
            }
            assertInventoryBatchExists(item.getFromInventoryBatchId());
            detail.setFromInventoryBatchId(item.getFromInventoryBatchId());
            detail.setSentQuantity(item.getSentQuantity());
        }

        transfer.setSentBy(request.getSentBy());
        transfer.setSentAt(LocalDateTime.now());
        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        StockTransfer saved = stockTransferRepository.save(transfer);

        inventoryService.transferOut(transferId);
        auditLogService.log(request.getSentBy(), transfer.getFromBranchId(), "TRANSFER_OUT", "StockTransfer",
                transferId, "APPROVED", "IN_TRANSIT", "Xác nhận gửi hàng");
        notificationService.create(null, transfer.getToBranchId(), "Có hàng điều chuyển đang đến",
                "Phiếu " + transfer.getTransferCode() + " đã được gửi", NotificationType.TRANSFER);
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer confirmReceived(Integer transferId, StockTransferReceiveRequest request) {
        StockTransfer transfer = getById(transferId);
        assertUserExists(request.getReceivedBy());
        if (transfer.getStatus() != StockTransferStatus.IN_TRANSIT) {
            throw new BusinessException("Chỉ được nhận hàng khi phiếu đang vận chuyển");
        }

        Map<Integer, StockTransferReceiveRequest.Item> itemMap = request.getItems().stream()
                .collect(Collectors.toMap(StockTransferReceiveRequest.Item::getStockTransferDetailId, i -> i));
        boolean hasVariance = false;

        for (StockTransferDetail detail : transfer.getDetails()) {
            StockTransferReceiveRequest.Item item = itemMap.get(detail.getStockTransferDetailId());
            if (item == null) {
                throw new BusinessException("Thiếu thông tin nhận cho thuốc ID: " + detail.getMedicineId());
            }
            if (detail.getSentQuantity() == null) {
                throw new BusinessException("Phiếu chưa có số lượng gửi hợp lệ");
            }
            if (item.getReceivedQuantity() > detail.getSentQuantity()) {
                throw new BusinessException("Số lượng nhận không được vượt số lượng đã gửi");
            }
            if (!item.getReceivedQuantity().equals(detail.getSentQuantity())) {
                hasVariance = true;
            }
            detail.setReceivedQuantity(item.getReceivedQuantity());
        }

        transfer.setReceivedBy(request.getReceivedBy());
        transfer.setReceivedAt(LocalDateTime.now());
        transfer.setStatus(StockTransferStatus.RECEIVED);
        StockTransfer saved = stockTransferRepository.save(transfer);

        inventoryService.transferIn(transferId);
        auditLogService.log(request.getReceivedBy(), transfer.getToBranchId(), "TRANSFER_IN", "StockTransfer",
                transferId, "IN_TRANSIT", "RECEIVED", hasVariance ? "Có chênh lệch nhận hàng" : "Nhận đủ hàng");
        if (hasVariance) {
            notificationService.create(null, transfer.getToBranchId(), "Chênh lệch điều chuyển",
                    "Phiếu " + transfer.getTransferCode() + " có số lượng nhận khác số lượng gửi", NotificationType.TRANSFER);
        }
        return saved;
    }

    @Override
    @Transactional
    public StockTransfer cancelTransfer(Integer transferId, Integer userId, String reason) {
        StockTransfer transfer = getById(transferId);
        if (transfer.getStatus() == StockTransferStatus.IN_TRANSIT || transfer.getStatus() == StockTransferStatus.RECEIVED) {
            throw new BusinessException("Không thể hủy phiếu đã gửi hoặc đã nhận hàng");
        }
        StockTransferStatus oldStatus = transfer.getStatus();
        transfer.setStatus(StockTransferStatus.CANCELLED);
        StockTransfer saved = stockTransferRepository.save(transfer);
        auditLogService.log(userId, transfer.getFromBranchId(), "CANCEL_TRANSFER", "StockTransfer",
                transferId, oldStatus.name(), "CANCELLED", reason);
        return saved;
    }

    @Override
    public StockTransfer getById(Integer transferId) {
        return stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu điều chuyển"));
    }

    @Override
    public List<StockTransfer> getAll() {
        return stockTransferRepository.findAll();
    }

    @Override
    public List<StockTransfer> getByBranch(Integer branchId) {
        return stockTransferRepository.findByFromBranchIdOrToBranchIdOrderByRequestedAtDesc(branchId, branchId);
    }

    private int calculateTransferValue(StockTransferCreateRequest request) {
        int total = 0;
        for (StockTransferCreateRequest.Item item : request.getItems()) {
            Integer salePrice = jdbcTemplate.queryForObject(
                    "SELECT sale_price FROM medicines WHERE medicine_id = ? AND status = 'ACTIVE'",
                    Integer.class,
                    item.getMedicineId()
            );
            if (salePrice == null) {
                throw new BusinessException("Không tìm thấy giá bán của thuốc ID: " + item.getMedicineId());
            }
            total += salePrice * item.getRequestedQuantity();
        }
        return total;
    }

    private int getIntSetting(String key, int defaultValue) {
        List<Integer> values = jdbcTemplate.query(
                "SELECT TRY_CAST(setting_value AS INT) FROM system_settings WHERE setting_key = ?",
                (rs, rowNum) -> rs.getInt(1), key
        );
        return values.isEmpty() ? defaultValue : values.get(0);
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

    private void assertMedicineExists(Integer medicineId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM medicines WHERE medicine_id = ? AND status = 'ACTIVE'", Integer.class, medicineId);
        if (count == null || count == 0) throw new ResourceNotFoundException("Không tìm thấy thuốc đang kinh doanh ID: " + medicineId);
    }

    private void assertInventoryBatchExists(Integer inventoryBatchId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM inventory_batches WHERE inventory_batch_id = ?", Integer.class, inventoryBatchId);
        if (count == null || count == 0) throw new ResourceNotFoundException("Không tìm thấy lô tồn kho ID: " + inventoryBatchId);
    }
}
