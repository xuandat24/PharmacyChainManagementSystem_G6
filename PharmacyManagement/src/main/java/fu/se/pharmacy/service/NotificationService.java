package fu.se.pharmacy.service;

import fu.se.pharmacy.common.enums.NotificationType;
import fu.se.pharmacy.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification create(Integer userId, Integer branchId, String title, String message, NotificationType type);
    void markAsRead(Integer notificationId);
    List<Notification> getRecent(Integer userId, Integer branchId);
}
