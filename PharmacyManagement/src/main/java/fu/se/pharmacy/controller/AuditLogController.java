package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.entity.AuditLog;
import fu.se.pharmacy.repository.AuditLogRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * FIX: Audit log chứa lịch sử thao tác nhân viên → chỉ Admin được xem.
 * Trước đây không có session check.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLog> latest(HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin");
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/target")
    public List<AuditLog> byTarget(HttpSession session,
                                   @RequestParam String targetType,
                                   @RequestParam Integer targetId) {
        AuthInterceptor.requireRole(session, "Admin");
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }
}
