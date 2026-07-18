package fu.se.pharmacy;

import fu.se.pharmacy.common.constants.SettingKeys;
import fu.se.pharmacy.entity.PurchaseRequest;
import fu.se.pharmacy.entity.PurchaseRequestDetail;
import fu.se.pharmacy.repository.PurchaseRequestDetailRepository;
import fu.se.pharmacy.repository.PurchaseRequestRepository;
import fu.se.pharmacy.service.AuditLogService;
import fu.se.pharmacy.service.SystemSettingService;
import fu.se.pharmacy.service.impl.PurchaseRequestServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho PurchaseRequestServiceImpl.
 * Business rules từ tài liệu RDS:
 *   UC-11: DRAFT → SUBMITTED/PENDING_ADMIN_APPROVAL (dựa vào PURCHASE_APPROVAL_LIMIT)
 *   UC-12: Admin duyệt SUBMITTED/PENDING_ADMIN_APPROVAL → APPROVED/PARTIALLY_APPROVED/REJECTED
 *   BR-18: Vượt PURCHASE_APPROVAL_LIMIT → PENDING_ADMIN_APPROVAL, cần Admin duyệt
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseRequestService - Unit Tests")
class PurchaseRequestServiceTest {

    @Mock private PurchaseRequestRepository purchaseRequestRepository;
    @Mock private PurchaseRequestDetailRepository detailRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemSettingService systemSettingService;

    @InjectMocks private PurchaseRequestServiceImpl purchaseRequestService;

    private PurchaseRequest draftRequest;
    private PurchaseRequestDetail detail1;

    @BeforeEach
    void setUp() {
        draftRequest = new PurchaseRequest();
        draftRequest.setPurchaseRequestId(1);
        draftRequest.setBranchId(1);
        draftRequest.setRequestedBy(2);
        draftRequest.setStatus("DRAFT");
        draftRequest.setTotalEstimatedAmount(3_000_000);

        detail1 = new PurchaseRequestDetail();
        detail1.setPurchaseRequestDetailId(10);
        detail1.setPurchaseRequestId(1);
        detail1.setMedicineId(5);
        detail1.setRequestedQuantity(100);
        detail1.setExpectedUnitPrice(30_000);
        detail1.setApprovedQuantity(0);
    }

    // ===========================================================
    // submitRequest
    // ===========================================================

    @Nested
    @DisplayName("submitRequest")
    class SubmitRequest {

        @Test
        @DisplayName("Gui duyet - tong duoi han muc - status SUBMITTED")
        void submitRequest_belowLimit_statusSubmitted() {
            draftRequest.setTotalEstimatedAmount(2_000_000);

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(systemSettingService.getMoneyLimit(eq(SettingKeys.PURCHASE_APPROVAL_LIMIT), any()))
                    .thenReturn(BigDecimal.valueOf(5_000_000));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            purchaseRequestService.submitRequest(1);

            verify(purchaseRequestRepository).save(argThat(r -> "SUBMITTED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Gui duyet - tong vuot han muc - status PENDING_ADMIN_APPROVAL (BR-18)")
        void submitRequest_aboveLimit_statusPendingAdminApproval() {
            draftRequest.setTotalEstimatedAmount(8_000_000);

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(systemSettingService.getMoneyLimit(eq(SettingKeys.PURCHASE_APPROVAL_LIMIT), any()))
                    .thenReturn(BigDecimal.valueOf(5_000_000));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            purchaseRequestService.submitRequest(1);

            verify(purchaseRequestRepository).save(argThat(r -> "PENDING_ADMIN_APPROVAL".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Gui duyet - khong o DRAFT - nem exception")
        void submitRequest_notDraft_throwsException() {
            draftRequest.setStatus("SUBMITTED");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));

            assertThatThrownBy(() -> purchaseRequestService.submitRequest(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");

            verify(purchaseRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("Gui duyet - tong bang dung han muc - SUBMITTED (gioi han tren la INCLUSIVE)")
        void submitRequest_exactlyAtLimit_statusSubmitted() {
            draftRequest.setTotalEstimatedAmount(5_000_000);

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(systemSettingService.getMoneyLimit(eq(SettingKeys.PURCHASE_APPROVAL_LIMIT), any()))
                    .thenReturn(BigDecimal.valueOf(5_000_000));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            purchaseRequestService.submitRequest(1);

            verify(purchaseRequestRepository).save(argThat(r -> "SUBMITTED".equals(r.getStatus())));
        }
    }

    // ===========================================================
    // approveRequest
    // ===========================================================

    @Nested
    @DisplayName("approveRequest")
    class ApproveRequest {

        @Test
        @DisplayName("Admin duyet day du - status APPROVED")
        void approveRequest_fullyApproved_statusApproved() {
            draftRequest.setStatus("SUBMITTED");
            detail1.setRequestedQuantity(100);

            PurchaseRequestDetail approved = new PurchaseRequestDetail();
            approved.setPurchaseRequestDetailId(10);
            approved.setApprovedQuantity(100); // duyet het

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(detailRepository.findByPurchaseRequestId(1)).thenReturn(List.of(detail1));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            purchaseRequestService.approveRequest(1, 99, "OK", List.of(approved));

            verify(purchaseRequestRepository).save(argThat(r -> "APPROVED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Admin duyet mot phan - status PARTIALLY_APPROVED")
        void approveRequest_partiallyApproved_statusPartiallyApproved() {
            draftRequest.setStatus("PENDING_ADMIN_APPROVAL");
            detail1.setRequestedQuantity(100);

            PurchaseRequestDetail approved = new PurchaseRequestDetail();
            approved.setPurchaseRequestDetailId(10);
            approved.setApprovedQuantity(50); // chi duyet 50/100

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(detailRepository.findByPurchaseRequestId(1)).thenReturn(List.of(detail1));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            purchaseRequestService.approveRequest(1, 99, "Chi duyet mot phan", List.of(approved));

            verify(purchaseRequestRepository).save(argThat(r -> "PARTIALLY_APPROVED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Admin duyet so luong = 0 tat ca - status REJECTED")
        void approveRequest_allZero_statusRejected() {
            draftRequest.setStatus("SUBMITTED");
            detail1.setRequestedQuantity(100);

            PurchaseRequestDetail approved = new PurchaseRequestDetail();
            approved.setPurchaseRequestDetailId(10);
            approved.setApprovedQuantity(0); // khong duyet

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(detailRepository.findByPurchaseRequestId(1)).thenReturn(List.of(detail1));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            purchaseRequestService.approveRequest(1, 99, "Tu choi", List.of(approved));

            verify(purchaseRequestRepository).save(argThat(r -> "REJECTED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Admin duyet so luong vuot requested - nem exception")
        void approveRequest_quantityOverRequested_throwsException() {
            draftRequest.setStatus("SUBMITTED");
            detail1.setRequestedQuantity(100);

            PurchaseRequestDetail overApproved = new PurchaseRequestDetail();
            overApproved.setPurchaseRequestDetailId(10);
            overApproved.setApprovedQuantity(150); // vuot 100

            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(detailRepository.findByPurchaseRequestId(1)).thenReturn(List.of(detail1));

            assertThatThrownBy(() -> purchaseRequestService.approveRequest(1, 99, "Test", List.of(overApproved)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Duyet yeu cau khong o SUBMITTED/PENDING - nem exception")
        void approveRequest_wrongStatus_throwsException() {
            draftRequest.setStatus("DRAFT");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));

            assertThatThrownBy(() -> purchaseRequestService.approveRequest(1, 99, "Test", List.of()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===========================================================
    // rejectRequest
    // ===========================================================

    @Nested
    @DisplayName("rejectRequest")
    class RejectRequest {

        @Test
        @DisplayName("Tu choi yeu cau SUBMITTED - status REJECTED")
        void rejectRequest_submitted_statusRejected() {
            draftRequest.setStatus("SUBMITTED");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(detailRepository.findByPurchaseRequestId(1)).thenReturn(List.of(detail1));
            when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any(), any());

            purchaseRequestService.rejectRequest(1, 99, "Gia cao");

            verify(purchaseRequestRepository).save(argThat(r -> "REJECTED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Tu choi yeu cau DRAFT - nem exception")
        void rejectRequest_draft_throwsException() {
            draftRequest.setStatus("DRAFT");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));

            assertThatThrownBy(() -> purchaseRequestService.rejectRequest(1, 99, "Test"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ===========================================================
    // cancelRequest
    // ===========================================================

    @Nested
    @DisplayName("cancelRequest")
    class CancelRequest {

        @Test
        @DisplayName("Huy yeu cau DRAFT - thanh cong")
        void cancelRequest_draft_succeeds() {
            draftRequest.setStatus("DRAFT");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            purchaseRequestService.cancelRequest(1);

            verify(purchaseRequestRepository).save(argThat(r -> "CANCELLED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Huy yeu cau SUBMITTED - thanh cong")
        void cancelRequest_submitted_succeeds() {
            draftRequest.setStatus("SUBMITTED");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));
            when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            purchaseRequestService.cancelRequest(1);

            verify(purchaseRequestRepository).save(argThat(r -> "CANCELLED".equals(r.getStatus())));
        }

        @Test
        @DisplayName("Huy yeu cau da xu ly xong (APPROVED) - nem exception")
        void cancelRequest_approved_throwsException() {
            draftRequest.setStatus("APPROVED");
            when(purchaseRequestRepository.findById(1)).thenReturn(Optional.of(draftRequest));

            assertThatThrownBy(() -> purchaseRequestService.cancelRequest(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("huy");
        }
    }
}
