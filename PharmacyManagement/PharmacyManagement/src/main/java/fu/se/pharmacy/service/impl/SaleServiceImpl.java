package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.dto.SaleDetailDTO;
import fu.se.pharmacy.dto.SaleDetailResponseDTO;
import fu.se.pharmacy.dto.SaleResponseDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.PrescriptionService;
import fu.se.pharmacy.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return saleRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SaleDTO> findByCustomerId(Integer customerId) {
        return saleRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String addItem(Integer saleId, Integer medicineId, Integer quantity) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"DRAFT".equals(sale.getStatus())) return "Hóa đơn không ở trạng thái DRAFT";

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc"));
        if (!"ACTIVE".equals(medicine.getStatus())) return "Thuốc ngừng kinh doanh";

        // Kiểm tra tồn kho
        Integer stock = inventoryBatchRepository.sumStock(sale.getBranchId(), medicineId);
        if (stock == null || stock < quantity)
            return "Tồn kho không đủ. Hiện có: " + (stock == null ? 0 : stock);

        // Kiểm tra thuốc kê đơn
        if (Boolean.TRUE.equals(medicine.getRequiresPrescription())) {
            if (sale.getCustomerId() == null)
                return "Thuốc kê đơn: vui lòng chọn khách hàng trước";
            if (sale.getPrescriptionId() == null)
                return "Thuốc kê đơn: vui lòng chọn đơn thuốc";
            if (!prescriptionService.isValidForSale(sale.getPrescriptionId(), medicineId, quantity))
                return "Thuốc không có trong đơn hoặc số lượng vượt quá đơn";
        }

        // Cộng dồn nếu đã có dòng
        Optional<SaleDetail> existing =
                saleDetailRepository.findBySaleIdAndMedicineId(saleId, medicineId);
        if (existing.isPresent()) {
            SaleDetail d = existing.get();
            d.setQuantity(d.getQuantity() + quantity);
            d.setLineAmount(d.getUnitPrice() * d.getQuantity());
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
        sale.setCustomerId(customerId);
        saleRepository.save(sale);
    }

    @Override
    public void setPrescription(Integer saleId, Integer prescriptionId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        sale.setPrescriptionId(prescriptionId);
        saleRepository.save(sale);
    }

    @Override
    @Transactional
    public void cancelDraft(Integer saleId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        if (!"DRAFT".equals(sale.getStatus()))
            throw new RuntimeException("Chỉ hủy được hóa đơn DRAFT");
        sale.setStatus("VOIDED");
        saleRepository.save(sale);
    }

    @Override
    @Transactional
    public void completeSale(Integer saleId) {
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        sale.setStatus("COMPLETED");
        saleRepository.save(sale);
        deductInventoryFifo(sale);
    }

    // ===== Private helpers =====

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

    /** FIFO: trừ lô gần hết hạn nhất trước */
    private void deductInventoryFifo(Sale sale) {
        for (SaleDetail detail : saleDetailRepository.findBySaleId(sale.getSaleId())) {
            List<InventoryBatch> batches = inventoryBatchRepository
                    .findByBranchIdAndMedicineIdAndStatusOrderByExpiryDateAsc(
                            sale.getBranchId(), detail.getMedicineId(), "AVAILABLE");
            int remaining = detail.getQuantity();
            for (InventoryBatch batch : batches) {
                if (remaining <= 0) break;
                int deduct = Math.min(batch.getQuantityOnHand(), remaining);
                batch.setQuantityOnHand(batch.getQuantityOnHand() - deduct);
                if (batch.getQuantityOnHand() == 0) batch.setStatus("DISPOSED");
                inventoryBatchRepository.save(batch);
                detail.setInventoryBatchId(batch.getInventoryBatchId());
                saleDetailRepository.save(detail);
                remaining -= deduct;
            }
        }
    }

    // ===== Converters =====

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

        // join tên pharmacist
        appUserRepository.findById(sale.getPharmacistId())
                .ifPresent(u -> dto.setPharmacistName(u.getFullName()));

        // join thông tin khách hàng
        if (sale.getCustomerId() != null) {
            customerRepository.findById(sale.getCustomerId()).ifPresent(c -> {
                dto.setCustomerName(c.getFullName());
                dto.setCustomerPhone(c.getPhone());
                dto.setCustomerAllergyNote(c.getAllergyNote());
            });
        }

        // join mã đơn thuốc
        if (sale.getPrescriptionId() != null) {
            prescriptionRepository.findById(sale.getPrescriptionId())
                    .ifPresent(p -> dto.setPrescriptionCode(p.getPrescriptionCode()));
        }

        // load chi tiết dòng hàng
        List<SaleDetailDTO> details = saleDetailRepository.findBySaleId(sale.getSaleId())
                .stream().map(this::toDetailDTO).collect(Collectors.toList());
        dto.setDetails(details);

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
        // join tên thuốc
        medicineRepository.findById(d.getMedicineId()).ifPresent(m -> {
            dto.setMedicineName(m.getMedicineName());
            dto.setMedicineUnit(m.getUnit());
        });
        return dto;
    }
}