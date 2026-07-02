package fu.se.pharmacy.service;

import fu.se.pharmacy.entity.AppUser;
import java.util.List;

public interface AppUserService {
    List<AppUser> getAllUsers();
    AppUser saveUser(AppUser user);
    AppUser getUserById(Integer id);
    // FIX: thêm performedByUserId để Audit Log ghi đúng ai đã thực hiện khóa tài khoản.
    void deleteUser(Integer id, Integer performedByUserId);
}
