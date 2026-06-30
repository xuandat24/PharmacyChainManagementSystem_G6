package fu.se2033.pharmacy.controller;

import fu.se2033.pharmacy.entity.AuditLog;
import fu.se2033.pharmacy.repository.AuditLogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class Flow4AuditLogController {
    private final AuditLogRepository auditLogRepository;

    public Flow4AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLog> latest() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @GetMapping("/target")
    public List<AuditLog> byTarget(@RequestParam String targetType, @RequestParam Integer targetId) {
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }
}
