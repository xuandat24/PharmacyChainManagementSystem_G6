package fu.se.pharmacy.service.impl;

import fu.se.pharmacy.entity.AppUser;
import fu.se.pharmacy.repository.AppUserRepository;
import fu.se.pharmacy.service.AppUserService;
import fu.se.pharmacy.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    // FIX: bổ sung ghi Audit Log khi khóa tài khoản — nằm trong danh sách hành động
    // bắt buộc ghi log của flow (mục VI.8) nhưng trước đây chưa được gọi.
    private final AuditLogService auditLogService;

    @Override
    public List<AppUser> getAllUsers() {
        return appUserRepository.findAll();
    }

    @Override
    public AppUser saveUser(AppUser user) {
        return appUserRepository.save(user);
    }

    @Override
    public AppUser getUserById(Integer id) {
        return appUserRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteUser(Integer id, Integer performedByUserId) {
        AppUser user = getUserById(id);
        if (user != null) {
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                throw new RuntimeException("Khong duoc phep khoa tai khoan Admin he thong!");
            }
            String oldStatus = user.getStatus();
            user.setStatus("INACTIVE");
            appUserRepository.save(user);
            auditLogService.log(performedByUserId, user.getBranchId(), "LOCK_ACCOUNT", "AppUser",
                    user.getUserId(), oldStatus, "INACTIVE", null);
        }
    }
}
