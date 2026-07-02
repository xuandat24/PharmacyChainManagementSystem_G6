package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.PeriodLockRequest;
import fu.se.pharmacy.dto.PeriodUnlockRequest;
import fu.se.pharmacy.entity.AccountingPeriod;
import fu.se.pharmacy.service.PeriodClosingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting-periods")
public class PeriodClosingController {
    private final PeriodClosingService periodClosingService;

    public PeriodClosingController(PeriodClosingService periodClosingService) {
        this.periodClosingService = periodClosingService;
    }

    // FIX: trước đây không kiểm tra vai trò — bất kỳ user nào đăng nhập (kể cả Pharmacist)
    // đều gọi được API khóa/mở kỳ kế toán. Chỉ Admin được phép khóa/mở sổ theo đúng flow.
    @PostMapping("/lock")
    public AccountingPeriod lock(@Valid @RequestBody PeriodLockRequest request, HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin");
        return periodClosingService.lockPeriod(request);
    }

    @PostMapping("/{id}/unlock")
    public AccountingPeriod unlock(@PathVariable Integer id, @Valid @RequestBody PeriodUnlockRequest request,
                                   HttpSession session) {
        AuthInterceptor.requireRole(session, "Admin");
        return periodClosingService.unlockPeriod(id, request);
    }
}
