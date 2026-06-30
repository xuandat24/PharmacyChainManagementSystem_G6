package fu.se2033.pharmacy.service.impl;

import fu.se2033.pharmacy.common.enums.NotificationType;
import fu.se2033.pharmacy.entity.Notification;
import fu.se2033.pharmacy.exception.ResourceNotFoundException;
import fu.se2033.pharmacy.repository.NotificationRepository;
import fu.se2033.pharmacy.service.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification create(Integer userId, Integer branchId, String title, String message, NotificationType type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setBranchId(branchId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getRecent(Integer userId, Integer branchId) {
        if (userId != null && branchId != null) {
            return notificationRepository.findTop50ByUserIdOrBranchIdOrderByCreatedAtDesc(userId, branchId);
        }
        if (userId != null) {
            return notificationRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId);
        }
        if (branchId != null) {
            return notificationRepository.findTop50ByBranchIdOrderByCreatedAtDesc(branchId);
        }
        return notificationRepository.findTop50ByOrderByCreatedAtDesc();
    }
}
