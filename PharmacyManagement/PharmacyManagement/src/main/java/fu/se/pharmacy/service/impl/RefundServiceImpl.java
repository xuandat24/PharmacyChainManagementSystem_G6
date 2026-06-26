package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.RefundRequestDTO;

import fu.se.pharmacy.entity.Payment;
import fu.se.pharmacy.entity.RefundRequest;
import fu.se.pharmacy.entity.Sale;
import fu.se.pharmacy.repository.AppUserRepository;
import fu.se.pharmacy.repository.PaymentRepository;
import fu.se.pharmacy.repository.RefundRequestRepository;
import fu.se.pharmacy.repository.SaleRepository;
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
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AppUserRepository appUserRepository;

    // Hạn mức Manager duyệt (2.000.000 đ)
    private static final int MANAGER_LIMIT = 2_000_000;

    @Override
    @Transactional
    public RefundRequestDTO createRequest(RefundRequestDTO dto, Integer requestedByUserId) {
        Sale sale = saleRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"COMPLETED".equals(sale.getStatus()))
            throw new RuntimeException("Chỉ hoàn tiền hóa đơn đã hoàn thành");

        RefundRequest req = new RefundRequest();
        req.setSaleId(dto.getSaleId());
        req.setRequestedBy(requestedByUserId);
        req.setReason(dto.getReason());
        req.setRefundAmount(sale.getFinalAmount());
        req.setStatus("PENDING");
        return toResponseDTO(refundRequestRepository.save(req));
    }

    @Override
    @Transactional
    public RefundRequestDTO approveByManager(Integer refundId, Integer managerId) {
        RefundRequest req = refundRequestRepository.findById(refundId).orElseThrow();

        if (req.getRefundAmount() > MANAGER_LIMIT)
            throw new RuntimeException("Số tiền vượt hạn mức Manager (" +
                    String.format("%,d", MANAGER_LIMIT) + " đ). Cần Admin duyệt.");

        Optional<Payment> payment = paymentRepository.findBySaleId(req.getSaleId());
        if (payment.isPresent() && "ONLINE".equals(payment.get().getPaymentMethod()))
            throw new RuntimeException("Thanh toán online cần Admin duyệt.");

        return doApprove(req, managerId);
    }

    @Override
    @Transactional
    public RefundRequestDTO approveByAdmin(Integer refundId, Integer adminId) {
        RefundRequest req = refundRequestRepository.findById(refundId).orElseThrow();
        return doApprove(req, adminId);
    }

    @Override
    @Transactional
    public RefundRequestDTO reject(Integer refundId, Integer reviewerId) {
        RefundRequest req = refundRequestRepository.findById(refundId).orElseThrow();
        req.setStatus("REJECTED");
        req.setApprovedBy(reviewerId);
        req.setApprovedAt(LocalDateTime.now());
        return toResponseDTO(refundRequestRepository.save(req));
    }

    @Override
    public List<RefundRequestDTO> findAll() {
        return refundRequestRepository.findAllByOrderByRequestedAtDesc()
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<RefundRequestDTO> findPending() {
        return refundRequestRepository.findByStatus("PENDING")
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<RefundRequestDTO> findById(Integer refundId) {
        return refundRequestRepository.findById(refundId).map(this::toResponseDTO);
    }

    // ===== Private helpers =====

    private RefundRequestDTO doApprove(RefundRequest req, Integer approverId) {
        req.setStatus("APPROVED");
        req.setApprovedBy(approverId);
        req.setApprovedAt(LocalDateTime.now());
        refundRequestRepository.save(req);

        // Chuyển sale → REFUNDED
        saleRepository.findById(req.getSaleId()).ifPresent(sale -> {
            sale.setStatus("REFUNDED");
            saleRepository.save(sale);
        });

        // Cập nhật payment → REFUNDED
        paymentRepository.findBySaleId(req.getSaleId()).ifPresent(p -> {
            p.setStatus("REFUNDED");
            paymentRepository.save(p);
        });

        return toResponseDTO(req);
    }

    // ===== Converter =====

    private RefundRequestDTO toResponseDTO(RefundRequest r) {
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

        // join tên người tạo
        appUserRepository.findById(r.getRequestedBy())
                .ifPresent(u -> dto.setRequestedByName(u.getFullName()));

        // join tên người duyệt
        if (r.getApprovedBy() != null) {
            appUserRepository.findById(r.getApprovedBy())
                    .ifPresent(u -> dto.setApprovedByName(u.getFullName()));
        }

        // join sale code
        saleRepository.findById(r.getSaleId())
                .ifPresent(s -> dto.setSaleCode(s.getSaleCode()));

        return dto;
    }
}