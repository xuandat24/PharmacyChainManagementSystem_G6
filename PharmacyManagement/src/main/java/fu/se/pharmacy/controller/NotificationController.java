package fu.se.pharmacy.controller;

import fu.se.pharmacy.entity.Notification;
import fu.se.pharmacy.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> recent(@RequestParam(required = false) Integer userId,
                                     @RequestParam(required = false) Integer branchId) {
        return notificationService.getRecent(userId, branchId);
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
    }
}
