package fu.se2033.pharmacy.repository;

import fu.se2033.pharmacy.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findTop50ByOrderByCreatedAtDesc();
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Notification> findTop50ByBranchIdOrderByCreatedAtDesc(Integer branchId);
    List<Notification> findTop50ByUserIdOrBranchIdOrderByCreatedAtDesc(Integer userId, Integer branchId);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Integer userId);
}
