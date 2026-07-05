package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.dto.CashShiftDTO;
import fu.se.pharmacy.entity.CashShift;
import fu.se.pharmacy.entity.Payment;
import fu.se.pharmacy.entity.Sale;
import fu.se.pharmacy.repository.AppUserRepository;
import fu.se.pharmacy.repository.CashShiftRepository;
import fu.se.pharmacy.repository.PaymentRepository;
import fu.se.pharmacy.repository.SaleRepository;
import fu.se.pharmacy.service.CashShiftService;
import fu.se.pharmacy.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CashShiftServiceImpl implements CashShiftService {

    @Autowired private CashShiftRepository cashShiftRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AppUserRepository appUserRepository;
    // FIX: đọc ngưỡng chênh lệch chốt ca từ system_settings thay vì hardcode.
    @Autowired private SystemSettingService systemSettingService;

    private static final BigDecimal DEFAULT_DIFF_THRESHOLD = BigDecimal.valueOf(500_000);

    @Override
    public CashShiftDTO openShift(Integer pharmacistId, Integer branchId) {
        Optional<CashShift> existing =
                cashShiftRepository.findByPharmacistIdAndStatus(pharmacistId, "OPEN");
        if (existing.isPresent()) return toResponseDTO(existing.get());

        CashShift shift = new CashShift();
        shift.setPharmacistId(pharmacistId);
        shift.setBranchId(branchId);
        shift.setStatus("OPEN");
        shift.setOpeningCashAmount(0);
        shift.setSystemCashAmount(0);
        return toResponseDTO(cashShiftRepository.save(shift));
    }

    @Override
    @Transactional
    public CashShiftDTO closeShift(Integer shiftId, CashShiftDTO dto) {
        CashShift shift = cashShiftRepository.findById(shiftId).orElseThrow(
                () -> new RuntimeException("Không tìm thấy ca"));
        if (!"OPEN".equals(shift.getStatus()))
            throw new RuntimeException("Ca không ở trạng thái OPEN");

        int systemCash = shift.getOpeningCashAmount();
        List<Sale> completedSales = saleRepository.findByBranchIdOrderBySaleDateDesc(shift.getBranchId());
        for (Sale sale : completedSales) {
            if (!"COMPLETED".equals(sale.getStatus())) continue;
            if (sale.getSaleDate() == null || sale.getSaleDate().isBefore(shift.getOpenedAt())) continue;

            // FIX: dùng findLatestBySaleId thay vì findBySaleId (tránh NonUniqueResult)
            Optional<Payment> payment = paymentRepository.findLatestBySaleId(sale.getSaleId());
            if (payment.isPresent()
                    && "CASH".equals(payment.get().getPaymentMethod())
                    && "PAID".equals(payment.get().getStatus())) {
                systemCash += sale.getFinalAmount();
            }
        }

        int actual = dto.getActualCashAmount();
        int diff = actual - systemCash;

        shift.setSystemCashAmount(systemCash);
        shift.setActualCashAmount(actual);
        shift.setDifferenceAmount(diff);
        shift.setClosedAt(LocalDateTime.now());
        shift.setNote(dto.getNote());
        BigDecimal threshold = systemSettingService.getMoneyLimit(
                SettingKeys.CASH_SHIFT_VARIANCE_LIMIT, DEFAULT_DIFF_THRESHOLD);
        shift.setStatus(BigDecimal.valueOf(Math.abs(diff)).compareTo(threshold) > 0
                ? "PENDING_ADMIN_REVIEW" : "CLOSED");

        return toResponseDTO(cashShiftRepository.save(shift));
    }

    @Override
    @Transactional
    public CashShiftDTO confirmShift(Integer shiftId, Integer managerId) {
        CashShift shift = cashShiftRepository.findById(shiftId).orElseThrow();
        shift.setStatus("CONFIRMED");
        shift.setManagerConfirmedBy(managerId);
        shift.setManagerConfirmedAt(LocalDateTime.now());
        return toResponseDTO(cashShiftRepository.save(shift));
    }

    @Override
    public Optional<CashShiftDTO> getOpenShift(Integer pharmacistId) {
        return cashShiftRepository.findByPharmacistIdAndStatus(pharmacistId, "OPEN")
                .map(this::toResponseDTO);
    }

    @Override
    public List<CashShiftDTO> findByBranchId(Integer branchId) {
        return cashShiftRepository.findByBranchIdOrderByOpenedAtDesc(branchId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<CashShiftDTO> findAll() {
        return cashShiftRepository.findAll()
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<CashShiftDTO> findPendingAdminReview() {
        return cashShiftRepository.findByStatus("PENDING_ADMIN_REVIEW")
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<CashShiftDTO> findById(Integer shiftId) {
        return cashShiftRepository.findById(shiftId).map(this::toResponseDTO);
    }

    private CashShiftDTO toResponseDTO(CashShift s) {
        CashShiftDTO dto = new CashShiftDTO();
        dto.setCashShiftId(s.getCashShiftId());
        dto.setBranchId(s.getBranchId());
        dto.setPharmacistId(s.getPharmacistId());
        dto.setOpenedAt(s.getOpenedAt());
        dto.setClosedAt(s.getClosedAt());
        dto.setOpeningCashAmount(s.getOpeningCashAmount());
        dto.setSystemCashAmount(s.getSystemCashAmount());
        dto.setActualCashAmount(s.getActualCashAmount());
        dto.setDifferenceAmount(s.getDifferenceAmount());
        dto.setStatus(s.getStatus());
        dto.setManagerConfirmedBy(s.getManagerConfirmedBy());
        dto.setManagerConfirmedAt(s.getManagerConfirmedAt());
        dto.setNote(s.getNote());

        appUserRepository.findById(s.getPharmacistId())
                .ifPresent(u -> dto.setPharmacistName(u.getFullName()));
        if (s.getManagerConfirmedBy() != null) {
            appUserRepository.findById(s.getManagerConfirmedBy())
                    .ifPresent(u -> dto.setManagerName(u.getFullName()));
        }
        return dto;
    }
}