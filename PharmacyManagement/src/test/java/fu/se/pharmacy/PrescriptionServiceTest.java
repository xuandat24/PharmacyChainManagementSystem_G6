package fu.se.pharmacy;

import fu.se.pharmacy.dto.PrescriptionDTO;
import fu.se.pharmacy.dto.PrescriptionDetailDTO;
import fu.se.pharmacy.entity.*;
import fu.se.pharmacy.repository.*;
import fu.se.pharmacy.service.impl.PrescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService - Unit Tests")
class PrescriptionServiceTest {

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private PrescriptionDetailRepository prescriptionDetailRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MedicineRepository medicineRepository;

    @InjectMocks private PrescriptionServiceImpl prescriptionService;

    private Prescription validPrescription;
    private Prescription expiredPrescription;
    private PrescriptionDetail amoxDetail;

    @BeforeEach
    void setUp() {
        validPrescription = new Prescription();
        validPrescription.setPrescriptionId(1);
        validPrescription.setCustomerId(2);
        validPrescription.setPrescriptionCode("RX-001");
        validPrescription.setDoctorName("BS. Nguyen Van Khoa");
        validPrescription.setClinicName("Benh vien Bach Mai");
        validPrescription.setPrescriptionDate(LocalDate.of(2026, 6, 10));
        validPrescription.setValidUntil(LocalDate.now().plusMonths(1)); // con han

        expiredPrescription = new Prescription();
        expiredPrescription.setPrescriptionId(2);
        expiredPrescription.setCustomerId(2);
        expiredPrescription.setPrescriptionCode("RX-OLD");
        expiredPrescription.setValidUntil(LocalDate.of(2020, 1, 1)); // da het han

        amoxDetail = new PrescriptionDetail();
        amoxDetail.setPrescriptionDetailId(1);
        amoxDetail.setPrescriptionId(1);
        amoxDetail.setMedicineId(5);
        amoxDetail.setPrescribedQuantity(21);
        amoxDetail.setDosageInstruction("Uong 1 vien x 3 lan/ngay x 7 ngay sau an");
    }

    // ===========================================================
    // isValidForSale
    // ===========================================================

    @Nested
    @DisplayName("isValidForSale")
    class IsValidForSale {

        @Test
        @DisplayName("Don con han, co thuoc, so luong du - tra ve true")
        void isValidForSale_valid_returnsTrue() {
            when(prescriptionRepository.findById(1)).thenReturn(Optional.of(validPrescription));
            when(prescriptionDetailRepository.findByPrescriptionIdAndMedicineId(1, 5))
                    .thenReturn(Optional.of(amoxDetail));

            boolean result = prescriptionService.isValidForSale(1, 5, 21);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Don het han - tra ve false")
        void isValidForSale_expiredPrescription_returnsFalse() {
            when(prescriptionRepository.findById(2)).thenReturn(Optional.of(expiredPrescription));

            boolean result = prescriptionService.isValidForSale(2, 5, 10);

            assertThat(result).isFalse();
            verify(prescriptionDetailRepository, never()).findByPrescriptionIdAndMedicineId(any(), any());
        }

        @Test
        @DisplayName("Thuoc khong co trong don - tra ve false")
        void isValidForSale_medicineNotInPrescription_returnsFalse() {
            when(prescriptionRepository.findById(1)).thenReturn(Optional.of(validPrescription));
            when(prescriptionDetailRepository.findByPrescriptionIdAndMedicineId(1, 99))
                    .thenReturn(Optional.empty());

            boolean result = prescriptionService.isValidForSale(1, 99, 5);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("So luong can vuot so luong trong don - tra ve false")
        void isValidForSale_requestedQtyExceeds_returnsFalse() {
            when(prescriptionRepository.findById(1)).thenReturn(Optional.of(validPrescription));
            when(prescriptionDetailRepository.findByPrescriptionIdAndMedicineId(1, 5))
                    .thenReturn(Optional.of(amoxDetail)); // chi ke 21

            boolean result = prescriptionService.isValidForSale(1, 5, 30); // can 30 > 21

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Don khong ton tai - tra ve false")
        void isValidForSale_prescriptionNotFound_returnsFalse() {
            when(prescriptionRepository.findById(999)).thenReturn(Optional.empty());

            boolean result = prescriptionService.isValidForSale(999, 5, 10);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("validUntil null (khong gioi han) - con hieu luc")
        void isValidForSale_nullValidUntil_isValid() {
            validPrescription.setValidUntil(null); // khong gioi han ngay het han

            when(prescriptionRepository.findById(1)).thenReturn(Optional.of(validPrescription));
            when(prescriptionDetailRepository.findByPrescriptionIdAndMedicineId(1, 5))
                    .thenReturn(Optional.of(amoxDetail));

            assertThat(prescriptionService.isValidForSale(1, 5, 10)).isTrue();
        }
    }

    // ===========================================================
    // findByCustomerId
    // ===========================================================

    @Test
    @DisplayName("findByCustomerId - tra ve danh sach don cua khach")
    void findByCustomerId_returnsList() {
        when(prescriptionRepository.findByCustomerId(2))
                .thenReturn(List.of(validPrescription, expiredPrescription));
        when(customerRepository.findById(2)).thenReturn(Optional.empty());

        List<PrescriptionDTO> result = prescriptionService.findByCustomerId(2);

        assertThat(result).hasSize(2);
        // Don con han
        assertThat(result.get(0).isValid()).isTrue();
        // Don het han
        assertThat(result.get(1).isValid()).isFalse();
    }

    @Test
    @DisplayName("findByCustomerId - khach khong co don tra ve danh sach rong")
    void findByCustomerId_noPresciptions_empty() {
        when(prescriptionRepository.findByCustomerId(99)).thenReturn(List.of());

        assertThat(prescriptionService.findByCustomerId(99)).isEmpty();
    }

    // ===========================================================
    // findById
    // ===========================================================

    @Test
    @DisplayName("findById - tim thay tra ve DTO day du voi chi tiet")
    void findById_found_returnsFullDTO() {
        when(prescriptionRepository.findById(1)).thenReturn(Optional.of(validPrescription));
        when(customerRepository.findById(2)).thenReturn(Optional.empty());
        when(prescriptionDetailRepository.findByPrescriptionId(1)).thenReturn(List.of(amoxDetail));
        when(medicineRepository.findById(5)).thenReturn(Optional.empty());

        Optional<PrescriptionDTO> result = prescriptionService.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getPrescriptionCode()).isEqualTo("RX-001");
        assertThat(result.get().getDetails()).hasSize(1);
        assertThat(result.get().isValid()).isTrue();
    }

    // ===========================================================
    // save
    // ===========================================================

    @Test
    @DisplayName("save - tao don thuoc moi - luu thanh cong")
    void save_createsNewPrescription() {
        PrescriptionDTO input = new PrescriptionDTO();
        input.setCustomerId(2);
        input.setPrescriptionCode("RX-NEW-001");
        input.setDoctorName("BS. Test");
        input.setClinicName("Phong kham Test");
        input.setPrescriptionDate(LocalDate.now());
        input.setValidUntil(LocalDate.now().plusMonths(1));

        Prescription saved = new Prescription();
        saved.setPrescriptionId(10);
        saved.setCustomerId(2);
        saved.setPrescriptionCode("RX-NEW-001");
        saved.setValidUntil(LocalDate.now().plusMonths(1));
        saved.setCreatedBy(5);

        when(prescriptionRepository.save(any())).thenReturn(saved);
        when(customerRepository.findById(2)).thenReturn(Optional.empty());

        PrescriptionDTO result = prescriptionService.save(input, 5);

        assertThat(result.getPrescriptionId()).isEqualTo(10);
        assertThat(result.getPrescriptionCode()).isEqualTo("RX-NEW-001");
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    // ===========================================================
    // addDetail
    // ===========================================================

    @Test
    @DisplayName("addDetail - them thuoc vao don - luu thanh cong")
    void addDetail_success() {
        PrescriptionDetailDTO input = new PrescriptionDetailDTO();
        input.setPrescriptionId(1);
        input.setMedicineId(5);
        input.setPrescribedQuantity(21);
        input.setDosageInstruction("Uong 1 vien x 3 lan/ngay");

        when(prescriptionDetailRepository.save(any())).thenReturn(amoxDetail);
        when(medicineRepository.findById(5)).thenReturn(Optional.empty());

        PrescriptionDetailDTO result = prescriptionService.addDetail(input);

        assertThat(result.getMedicineId()).isEqualTo(5);
        assertThat(result.getPrescribedQuantity()).isEqualTo(21);
        verify(prescriptionDetailRepository).save(any(PrescriptionDetail.class));
    }

    // ===========================================================
    // findDetails
    // ===========================================================

    @Test
    @DisplayName("findDetails - tra ve danh sach thuoc trong don voi ten thuoc")
    void findDetails_returnDetailsWithMedicineName() {
        Medicine medicine = new Medicine();
        medicine.setMedicineId(5);
        medicine.setMedicineName("Amoxicillin 500mg");
        medicine.setUnit("vien");

        when(prescriptionDetailRepository.findByPrescriptionId(1))
                .thenReturn(List.of(amoxDetail));
        when(medicineRepository.findById(5)).thenReturn(Optional.of(medicine));

        List<PrescriptionDetailDTO> result = prescriptionService.findDetails(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMedicineName()).isEqualTo("Amoxicillin 500mg");
        assertThat(result.get(0).getMedicineUnit()).isEqualTo("vien");
    }
}
