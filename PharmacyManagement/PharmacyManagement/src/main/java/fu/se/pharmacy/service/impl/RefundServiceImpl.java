package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.RefundRequestDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RefundServiceImpl implements RefundService {

    @Autowired private RefundRequestRepository refundRequestRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleDetailRepository saleDetailRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private AppUserRepository appUserRepository;

    private static final int MANAGER_LIMIT = 2_000_000;

    @Override
    @Transactional
    public RefundRequestDTO createRequest(RefundRequestDTO dto, Integer requestedByUserId) {
        Sale sale = saleRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"COMPLETED".equals(sale.getStatus()))
            throw new RuntimeException("Chỉ hoàn tiền hóa đơn đã hoàn thành (COMPLETED)");

        boolean hasPending = refundRequestRepository.findBySaleId(dto.getSaleId())
                .stream().anyMatch(r -> "PENDING".equals(r.getStatus()));
        if (hasPending)
            throw new RuntimeException("Hóa đơn này đã có yêu cầu hoàn tiền đang chờ xử lý (PENDING)");

        RefundRequest req = new RefundRequest();
        req.setSaleId(dto.getSaleId());
        req.setRequestedBy(requestedByUserId);
        req.setReason(dto.getReason());
        req.setRefundAmount(sale.getFinalAmount());
        req.setStatus("PENDING");
        return toDTO(refundRequestRepository.save(req));
    }

    @Override
    @Transactional
    public RefundRequestDTO approveByManager(Integer refundId, Integer managerId) {
        RefundRequest req = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));
        if (!"PENDING".equals(req.getStatus()))
            throw new RuntimeException("Yêu cầu không ở trạng thái PENDING");

        if (req.getRefundAmount() > MANAGER_LIMIT)
            throw new RuntimeException("Số tiền vượt hạn mức Manager ("
                    + String.format("%,d", MANAGER_LIMIT) + " đ). Cần Admin duyệt.");

        // FIX: dùng findLatestBySaleId thay vì findBySaleId (tránh NonUniqueResult)
        Optional<Payment> payment = paymentRepository.findLatestBySaleId(req.getSaleId());
        if (payment.isPresent() && "ONLINE".equals(payment.get().getPaymentMethod()))
            throw new RuntimeException("Thanh toán online cần Admin duyệt.");

        return doApprove(req, managerId);
    }

    @Override
    @Transactional
    public RefundRequestDTO approveByAdmin(Integer refundId, Integer adminId) {
        RefundRequest req = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));
        if (!"PENDING".equals(req.getStatus()))
            throw new RuntimeException("Yêu cầu không ở trạng thái PENDING");
        return doApprove(req, adminId);
    }

    @Override
    @Transactional
    public RefundRequestDTO reject(Integer refundId, Integer reviewerId) {
        RefundRequest req = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn tiền"));
        if (!"PENDING".equals(req.getStatus()))
            throw new RuntimeException("Yêu cầu không ở trạng thái PENDING");
        req.setStatus("REJECTED");
        req.setApprovedBy(reviewerId);
        req.setApprovedAt(LocalDateTime.now());
        return toDTO(refundRequestRepository.save(req));
    }

    @Override
    public List<RefundRequestDTO> findAll() {
        return refundRequestRepository.findAllByOrderByRequestedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundRequestDTO> findPending() {
        return refundRequestRepository.findByStatus("PENDING")
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<RefundRequestDTO> findById(Integer refundId) {
        return refundRequestRepository.findById(refundId).map(this::toDTO);
    }

    // ========================================================
    // Private helpers
    // ========================================================

    private RefundRequestDTO doApprove(RefundRequest req, Integer approverId) {
        req.setStatus("APPROVED");
        req.setApprovedBy(approverId);
        req.setApprovedAt(LocalDateTime.now());
        refundRequestRepository.save(req);

        saleRepository.findById(req.getSaleId()).ifPresent(sale -> {
            sale.setStatus("REFUNDED");
            saleRepository.save(sale);
            restoreInventory(sale);
        });

        // FIX: dùng findLatestBySaleId thay vì findBySaleId (tránh NonUniqueResult)
        paymentRepository.findLatestBySaleId(req.getSaleId()).ifPresent(p -> {
            p.setStatus("REFUNDED");
            paymentRepository.save(p);
        });

        return toDTO(req);
    }

    private void restoreInventory(Sale sale) {
        List<SaleDetail> details = saleDetailRepository.findBySaleId(sale.getSaleId());
        for (SaleDetail detail : details) {
            if (detail.getInventoryBatchId() != null) {
                inventoryBatchRepository.findById(detail.getInventoryBatchId()).ifPresent(batch -> {
                    batch.setQuantityOnHand(batch.getQuantityOnHand() + detail.getQuantity());
                    if ("DISPOSED".equals(batch.getStatus())) batch.setStatus("AVAILABLE");
                    inventoryBatchRepository.save(batch);
                });
            }
        }
    }

    // ========================================================
    // Converter
    // ========================================================

    private RefundRequestDTO toDTO(RefundRequest r) {
        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setRefundRequestId(r.getRefundRequestId());
        dto.setSaleId(r.getSaleId());
        dto.setRequestedBy(r.getRequestedBy());
        dto.setApprovedBy(r.getApprovedBy());
        dto.setRequestedAt(r.getRequestedAt());
        dto.setApprovedAt(r.getApprovedAt());
        dto.setRefundAmount(r.getRefundAmount());
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());

        appUserRepository.findById(r.getRequestedBy())
                .ifPresent(u -> dto.setRequestedByName(u.getFullName()));

        if (r.getApprovedBy() != null)
            appUserRepository.findById(r.getApprovedBy())
                    .ifPresent(u -> dto.setApprovedByName(u.getFullName()));

        saleRepository.findById(r.getSaleId()).ifPresent(s -> {
            dto.setSaleCode(s.getSaleCode());
            dto.setSaleTotal(s.getFinalAmount());
        });

        // FIX: dùng findLatestBySaleId thay vì findBySaleId
        paymentRepository.findLatestBySaleId(r.getSaleId())
                .ifPresent(p -> dto.setPaymentMethod(p.getPaymentMethod()));

        return dto;
    }
}