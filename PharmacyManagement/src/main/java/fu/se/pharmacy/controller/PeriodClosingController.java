package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.PeriodLockRequest;
import fu.se.pharmacy.dto.PeriodUnlockRequest;
import fu.se.pharmacy.entity.AccountingPeriod;
import fu.se.pharmacy.service.PeriodClosingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting-periods")
public class PeriodClosingController {
    private final PeriodClosingService periodClosingService;

    public PeriodClosingController(PeriodClosingService periodClosingService) {
        this.periodClosingService = periodClosingService;
    }

    @PostMapping("/lock")
    public AccountingPeriod lock(@Valid @RequestBody PeriodLockRequest request) {
        return periodClosingService.lockPeriod(request);
    }

    @PostMapping("/{id}/unlock")
    public AccountingPeriod unlock(@PathVariable Integer id, @Valid @RequestBody PeriodUnlockRequest request) {
        return periodClosingService.unlockPeriod(id, request);
    }
}
