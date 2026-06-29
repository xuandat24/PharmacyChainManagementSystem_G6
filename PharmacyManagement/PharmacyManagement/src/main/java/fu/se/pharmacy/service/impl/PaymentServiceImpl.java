package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.PaymentDTO;
import fu.se.pharmacy.entity.Payment;
import fu.se.pharmacy.entity.PaymentTransaction;
import fu.se.pharmacy.entity.Sale;
import fu.se.pharmacy.repository.PaymentRepository;
import fu.se.pharmacy.repository.PaymentTransactionRepository;
import fu.se.pharmacy.repository.SaleRepository;
import fu.se.pharmacy.service.PaymentService;
import fu.se.pharmacy.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired private SaleRepository saleRepository;

    @Autowired @Lazy private SaleService saleService;

    @Override
    @Transactional
    public PaymentDTO processCash(PaymentDTO dto) {
        Sale sale = saleRepository.findById(dto.getSaleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Hóa đơn không ở trạng thái DRAFT");

        // FIX: ngăn tạo duplicate - kiểm tra đã có payment PAID chưa
        List<Payment> existing = paymentRepository.findBySaleIdOrderByCreatedAtDesc(dto.getSaleId());
        if (!existing.isEmpty() && "PAID".equals(existing.get(0).getStatus())) {
            throw new RuntimeException("Hóa đơn này đã được thanh toán.");
        }

        if (dto.getCustomerPaidAmount() == null || dto.getCustomerPaidAmount() < sale.getFinalAmount())
            throw new RuntimeException("Tiền khách đưa không đủ. Cần: "
                    + String.format("%,d", sale.getFinalAmount()) + " đ");

        Payment payment = new Payment();
        payment.setSaleId(dto.getSaleId());
        payment.setPaymentMethod("CASH");
        payment.setAmount(sale.getFinalAmount());
        payment.setCustomerPaidAmount(dto.getCustomerPaidAmount());
        payment.setChangeAmount(dto.getCustomerPaidAmount() - sale.getFinalAmount());
        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        saleService.completeSale(dto.getSaleId());
        return toDTO(payment);
    }

    @Override
    @Transactional
    public PaymentDTO createOnlinePayment(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        // FIX: dùng findLatestBySaleId thay vì findBySaleId (tránh NonUniqueResult)
        Optional<Payment> existing = paymentRepository.findLatestBySaleId(saleId);
        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus()))
            return toDTO(existing.get());

        Payment payment = new Payment();
        payment.setSaleId(saleId);
        payment.setPaymentMethod("ONLINE");
        payment.setAmount(sale.getFinalAmount());
        payment.setStatus("PENDING");
        payment.setNote("PHARMACY_PAY_" + saleId + "_" + sale.getFinalAmount());
        return toDTO(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public void handleCallback(Integer paymentId, String gatewayCode, Integer amount, String rawMsg) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        if (!"PENDING".equals(payment.getStatus()))
            throw new RuntimeException("Payment không ở trạng thái PENDING");
        if (!payment.getAmount().equals(amount))
            throw new RuntimeException("Số tiền callback không khớp. Kỳ vọng: "
                    + payment.getAmount() + ", nhận được: " + amount);

        if (paymentTransactionRepository.existsByGatewayTransactionCode(gatewayCode))
            throw new RuntimeException("Giao dịch " + gatewayCode + " đã được xử lý trước đó");

        PaymentTransaction tx = new PaymentTransaction();
        tx.setPaymentId(paymentId);
        tx.setGatewayTransactionCode(gatewayCode);
        tx.setAmount(amount);
        tx.setTransactionStatus("SUCCESS");
        tx.setRawMessage(rawMsg);
        paymentTransactionRepository.save(tx);

        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        saleService.completeSale(payment.getSaleId());
    }

    @Override
    public Optional<PaymentDTO> findBySaleId(Integer saleId) {
        // FIX: dùng findLatestBySaleId thay vì findBySaleId (tránh NonUniqueResult)
        return paymentRepository.findLatestBySaleId(saleId).map(this::toDTO);
    }

    private PaymentDTO toDTO(Payment p) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(p.getPaymentId());
        dto.setSaleId(p.getSaleId());
        dto.setPaymentCode(p.getPaymentCode());
        dto.setPaymentMethod(p.getPaymentMethod());
        dto.setStatus(p.getStatus());
        dto.setAmount(p.getAmount());
        dto.setCustomerPaidAmount(p.getCustomerPaidAmount());
        dto.setChangeAmount(p.getChangeAmount());
        dto.setQrNote(p.getNote());
        dto.setPaidAt(p.getPaidAt());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}