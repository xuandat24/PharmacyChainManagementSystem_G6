package fu.se.pharmacy.service;

public interface AuditLogService {
    void log(Integer userId, Integer branchId, String action, String targetType, Integer targetId, String oldValue, String newValue, String reason);
}
