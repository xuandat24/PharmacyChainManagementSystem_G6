package fu.se2033.pharmacy.repository;

import fu.se2033.pharmacy.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Integer targetId);
}
