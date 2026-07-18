package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.dto.SaleDetailDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.InventoryService;
import fu.se.pharmacy.service.PeriodClosingService;
import fu.se.pharmacy.service.PrescriptionService;
import fu.se.pharmacy.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SaleServiceImpl implements SaleService {

    @Autowired private SaleRepository saleRepository;
    @Autowired private SaleDetailRepository saleDetailRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PrescriptionService prescriptionService;

    // Repository của Người 2 — chỉ đọc (kiểm tra tồn), KHÔNG tự sửa entity kho.
    // Mọi thay đổi số lượng tồn phải đi qua InventoryService (Người 2 sở hữu).
    @Autowired private MedicineRepository medicineRepository;
    @Autowired private InventoryBatchRepository inventoryBatchRepository;
    @Autowired private InventoryService inventoryService;
    // FIX: kiểm tra kỳ kế toán đã khóa trước khi hoàn tất bán hàng + ghi audit log khi hủy hóa đơn.
    @Autowired private PeriodClosingService periodClosingService;
    @Autowired private AuditLogService auditLogService;

    // ========================================================
    // CRUD cơ bản
    // ========================================================

    @Override
    public SaleDTO getOrCreateDraft(Integer pharmacistId, Integer branchId) {
        List<Sale> drafts = saleRepository.findByPharmacistIdAndStatus(pharmacistId, "DRAFT");
        Sale sale = drafts.isEmpty() ? createDraft(pharmacistId, branchId) : drafts.get(0);
        return toResponseDTO(sale);
    }

    @Override
    public Optional<SaleDTO> findById(Integer saleId) {
        return saleRepository.findById(saleId).map(this::toResponseDTO);
    }

    @Override
    public List<SaleDTO> findAll() {
        return saleRepository.findAllByOrderBySaleDateDesc()
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<SaleDTO> findByCustomerId(Integer customerId) {
        return saleRepository.findByCustomerId(customerId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<SaleDTO> findByBranchId(Integer branchId) {
        return saleRepository.findByBranchIdOrderBySaleDateDesc(branchId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    // ========================================================
    // Thao tác giỏ hàng
    // ========================================================

    @Override
    @Transactional
    public String addItem(Integer saleId, Integer medicineId, Integer quantity) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"DRAFT".equals(sale.getStatus())) return "Hóa đơn không ở trạng thái DRAFT";

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));
        if (!"ACTIVE".equals(medicine.getStatus())) return "Thuốc đã ngừng kinh doanh";

        // Kiểm tra tồn kho
        Integer stock = inventoryBatchRepository.sumStock(sale.getBranchId(), medicineId);
        if (stock == null || stock < quantity)
            return "Tồn kho không đủ. Hiện có: " + (stock == null ? 0 : stock) + " " + medicine.getUnit();

        // Kiểm tra thuốc kê đơn (Rx)
        if (Boolean.TRUE.equals(medicine.getRequiresPrescription())) {
            if (sale.getCustomerId() == null)
                return "Thuốc kê đơn [" + medicine.getMedicineName() + "]: vui lòng chọn khách hàng trước";
            if (sale.getPrescriptionId() == null)
                return "Thuốc kê đơn [" + medicine.getMedicineName() + "]: vui lòng chọn đơn thuốc";
            if (!prescriptionService.isValidForSale(sale.getPrescriptionId(), medicineId, quantity))
                return "Thuốc không có trong đơn hoặc số lượng vượt quá đơn thuốc";
        }

        // Cộng dồn nếu đã có dòng cùng thuốc
        Optional<SaleDetail> existing =
                saleDetailRepository.findBySaleIdAndMedicineId(saleId, medicineId);
        if (existing.isPresent()) {
            SaleDetail d = existing.get();
            int newQty = d.getQuantity() + quantity;
            // Kiểm tra lại tồn kho cho tổng mới
            if (stock < newQty)
                return "Tồn kho không đủ. Đã có " + d.getQuantity() + ", cần thêm " + quantity
                        + " nhưng chỉ còn " + stock;
            d.setQuantity(newQty);
            d.setLineAmount(d.getUnitPrice() * newQty);
            saleDetailRepository.save(d);
        } else {
            SaleDetail d = new SaleDetail();
            d.setSaleId(saleId);
            d.setMedicineId(medicineId);
            d.setQuantity(quantity);
            d.setUnitPrice(medicine.getSalePrice());
            d.setLineAmount(medicine.getSalePrice() * quantity);
            saleDetailRepository.save(d);
        }

        recalcTotal(sale);
        return null; // null = thành công
    }

    @Override
    @Transactional
    public void updateItemQuantity(Integer saleDetailId, Integer quantity) {
        SaleDetail d = saleDetailRepository.findById(saleDetailId).orElseThrow();
        Integer saleId = d.getSaleId();
        if (quantity <= 0) {
            saleDetailRepository.deleteById(saleDetailId);
        } else {
            d.setQuantity(quantity);
            d.setLineAmount(d.getUnitPrice() * quantity);
            saleDetailRepository.save(d);
        }
        recalcTotal(saleRepository.findById(saleId).orElseThrow());
    }

    @Override
    @Transactional
    public void removeItem(Integer saleDetailId) {
        SaleDetail d = saleDetailRepository.findById(saleDetailId).orElseThrow();
        Integer saleId = d.getSaleId();
        saleDetailRepository.deleteById(saleDetailId);
        recalcTotal(saleRepository.findById(saleId).orElseThrow());
    }

    @Override
    public void setCustomer(Integer saleId, Integer customerId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        // FIX: chỉ cho phép thay đổi khi DRAFT
        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Không thể đổi khách hàng sau khi hóa đơn đã xử lý");
        sale.setCustomerId(customerId);
        saleRepository.save(sale);
    }

    @Override
    public void setPrescription(Integer saleId, Integer prescriptionId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        // FIX: chỉ cho phép gán đơn thuốc khi DRAFT
        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Không thể đổi đơn thuốc sau khi hóa đơn đã xử lý");
        sale.setPrescriptionId(prescriptionId);
        saleRepository.save(sale);
    }

    @Override
    @Transactional
    public void cancelDraft(Integer saleId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Chỉ hủy được hóa đơn ở trạng thái DRAFT");
        sale.setStatus("VOIDED");
        saleRepository.save(sale);
        auditLogService.log(sale.getPharmacistId(), sale.getBranchId(), "SALE_CANCEL_DRAFT",
                "Sale", saleId, "DRAFT", "VOIDED", null);
    }

    @Override
    @Transactional
    public void completeSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn: " + saleId));
        // FIX: chỉ hoàn tất hóa đơn ở trạng thái DRAFT (tránh double-complete)
        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Hóa đơn đã được xử lý (status: " + sale.getStatus() + ")");
        if (periodClosingService.isDateLocked(LocalDate.now())) {
            throw new RuntimeException("Kỳ kế toán hiện tại đã bị khóa, không thể hoàn tất hóa đơn.");
        }
        sale.setStatus("COMPLETED");
        saleRepository.save(sale);
        // FIX: trước đây tự trừ tồn kho trực tiếp (deductInventoryFifo) — vi phạm nguyên tắc
        // "chỉ InventoryService được cập nhật kho" và không ghi InventoryTransaction (làm sai
        // báo cáo nhập-xuất-tồn). Nay gọi đúng InventoryService của Người 2.
        inventoryService.deductForSale(saleId);
    }

    // ========================================================
    // Private helpers
    // ========================================================

    private Sale createDraft(Integer pharmacistId, Integer branchId) {
        Sale sale = new Sale();
        sale.setPharmacistId(pharmacistId);
        sale.setBranchId(branchId);
        sale.setStatus("DRAFT");
        sale.setTotalAmount(0);
        sale.setDiscountAmount(0);
        sale.setFinalAmount(0);
        return saleRepository.save(sale);
    }

    private void recalcTotal(Sale sale) {
        int total = saleDetailRepository.findBySaleId(sale.getSaleId())
                .stream().mapToInt(SaleDetail::getLineAmount).sum();
        sale.setTotalAmount(total);
        sale.setFinalAmount(total - sale.getDiscountAmount());
        saleRepository.save(sale);
    }

    // ========================================================
    // Converters
    // ========================================================

    private SaleDTO toResponseDTO(Sale sale) {
        SaleDTO dto = new SaleDTO();
        dto.setSaleId(sale.getSaleId());
        dto.setSaleCode(sale.getSaleCode());
        dto.setBranchId(sale.getBranchId());
        dto.setPharmacistId(sale.getPharmacistId());
        dto.setCustomerId(sale.getCustomerId());
        dto.setPrescriptionId(sale.getPrescriptionId());
        dto.setSaleDate(sale.getSaleDate());
        dto.setStatus(sale.getStatus());
        dto.setTotalAmount(sale.getTotalAmount());
        dto.setDiscountAmount(sale.getDiscountAmount());
        dto.setFinalAmount(sale.getFinalAmount());
        dto.setNote(sale.getNote());

        // Join tên pharmacist
        appUserRepository.findById(sale.getPharmacistId())
                .ifPresent(u -> dto.setPharmacistName(u.getFullName()));

        // Join thông tin khách hàng
        if (sale.getCustomerId() != null) {
            customerRepository.findById(sale.getCustomerId()).ifPresent(c -> {
                dto.setCustomerName(c.getFullName());
                dto.setCustomerPhone(c.getPhone());
                dto.setCustomerAllergyNote(c.getAllergyNote());
            });
        }

        // Join mã đơn thuốc
        if (sale.getPrescriptionId() != null) {
            prescriptionRepository.findById(sale.getPrescriptionId())
                    .ifPresent(p -> dto.setPrescriptionCode(p.getPrescriptionCode()));
        }

        // Load chi tiết dòng hàng
        dto.setDetails(saleDetailRepository.findBySaleId(sale.getSaleId())
                .stream().map(this::toDetailDTO).collect(Collectors.toList()));

        return dto;
    }

    private SaleDetailDTO toDetailDTO(SaleDetail d) {
        SaleDetailDTO dto = new SaleDetailDTO();
        dto.setSaleDetailId(d.getSaleDetailId());
        dto.setSaleId(d.getSaleId());
        dto.setMedicineId(d.getMedicineId());
        dto.setInventoryBatchId(d.getInventoryBatchId());
        dto.setQuantity(d.getQuantity());
        dto.setUnitPrice(d.getUnitPrice());
        dto.setLineAmount(d.getLineAmount());
        // Join tên thuốc và đơn vị
        medicineRepository.findById(d.getMedicineId()).ifPresent(m -> {
            dto.setMedicineName(m.getMedicineName());
            dto.setMedicineUnit(m.getUnit());
            dto.setRequiresPrescription(m.getRequiresPrescription());
        });
        return dto;
    }
}