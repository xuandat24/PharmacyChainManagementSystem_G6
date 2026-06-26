package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.dto.PrescriptionDetailDTO;
import fu.se.pharmacy.dto.PrescriptionDTO;
import fu.se.pharmacy.entity.Customer;
import fu.se.pharmacy.entity.Prescription;
import fu.se.pharmacy.entity.PrescriptionDetail;
import fu.se.pharmacy.repository.CustomerRepository;
import fu.se.pharmacy.repository.PrescriptionDetailRepository;
import fu.se.pharmacy.repository.PrescriptionRepository;
import fu.se.pharmacy.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private PrescriptionDetailRepository prescriptionDetailRepository;
    @Autowired private CustomerRepository customerRepository;

    @Override
    public List<PrescriptionDTO> findByCustomerId(Integer customerId) {
        return prescriptionRepository.findByCustomerId(customerId)
                .stream()
                .map(p -> toResponseDTO(p, false))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PrescriptionDTO> findById(Integer prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .map(p -> toResponseDTO(p, true)); // true = load chi tiết thuốc
    }

    @Override
    public Optional<Prescription> findEntityById(Integer prescriptionId) {
        return prescriptionRepository.findById(prescriptionId);
    }

    @Override
    public PrescriptionDTO save(PrescriptionDTO dto, Integer createdByUserId) {
        Prescription p = new Prescription();
        p.setCustomerId(dto.getCustomerId());
        p.setPrescriptionCode(dto.getPrescriptionCode());
        p.setDoctorName(dto.getDoctorName());
        p.setClinicName(dto.getClinicName());
        p.setPrescriptionDate(dto.getPrescriptionDate());
        p.setValidUntil(dto.getValidUntil());
        p.setNote(dto.getNote());
        p.setCreatedBy(createdByUserId);
        return toResponseDTO(prescriptionRepository.save(p), false);
    }

    @Override
    public PrescriptionDetailDTO addDetail(PrescriptionDetailDTO dto) {
        PrescriptionDetail detail = new PrescriptionDetail();
        detail.setPrescriptionId(dto.getPrescriptionId());
        detail.setMedicineId(dto.getMedicineId());
        detail.setPrescribedQuantity(dto.getPrescribedQuantity());
        detail.setDosageInstruction(dto.getDosageInstruction());
        return toDetailDTO(prescriptionDetailRepository.save(detail));
    }

    @Override
    public List<PrescriptionDetailDTO> findDetails(Integer prescriptionId) {
        return prescriptionDetailRepository.findByPrescriptionId(prescriptionId)
                .stream()
                .map(this::toDetailDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isValidForSale(Integer prescriptionId, Integer medicineId, Integer requestedQty) {
        Optional<Prescription> presc = prescriptionRepository.findById(prescriptionId);
        if (presc.isEmpty() || !presc.get().isValid()) return false;

        Optional<PrescriptionDetail> detail =
                prescriptionDetailRepository.findByPrescriptionIdAndMedicineId(prescriptionId, medicineId);
        return detail.isPresent() && detail.get().getPrescribedQuantity() >= requestedQty;
    }

    // ===== Converters =====

    private PrescriptionDTO toResponseDTO(Prescription p, boolean withDetails) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setPrescriptionId(p.getPrescriptionId());
        dto.setCustomerId(p.getCustomerId());
        dto.setPrescriptionCode(p.getPrescriptionCode());
        dto.setDoctorName(p.getDoctorName());
        dto.setClinicName(p.getClinicName());
        dto.setPrescriptionDate(p.getPrescriptionDate());
        dto.setValidUntil(p.getValidUntil());
        dto.setValid(p.isValid());
        dto.setNote(p.getNote());
        dto.setCreatedAt(p.getCreatedAt());

        // join tên khách hàng
        customerRepository.findById(p.getCustomerId())
                .map(Customer::getFullName)
                .ifPresent(dto::setCustomerName);

        // load chi tiết thuốc nếu cần
        if (withDetails) {
            dto.setDetails(prescriptionDetailRepository.findByPrescriptionId(p.getPrescriptionId())
                    .stream().map(this::toDetailDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    private PrescriptionDetailDTO toDetailDTO(PrescriptionDetail d) {
        PrescriptionDetailDTO dto = new PrescriptionDetailDTO();
        dto.setPrescriptionDetailId(d.getPrescriptionDetailId());
        dto.setPrescriptionId(d.getPrescriptionId());
        dto.setMedicineId(d.getMedicineId());
        dto.setPrescribedQuantity(d.getPrescribedQuantity());
        dto.setDosageInstruction(d.getDosageInstruction());
        return dto;
    }
}