package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.AuditLog;
import fu.se.pharmacy.repository.AuditLogRepository;
import fu.se.pharmacy.service.AuditLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void log(Integer userId, Integer branchId, String action, String targetType, Integer targetId,
                    String oldValue, String newValue, String reason) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setBranchId(branchId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
