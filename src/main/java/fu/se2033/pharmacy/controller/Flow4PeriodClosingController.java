package fu.se2033.pharmacy.controller;

import fu.se2033.pharmacy.dto.flow4.PeriodLockRequest;
import fu.se2033.pharmacy.dto.flow4.PeriodUnlockRequest;
import fu.se2033.pharmacy.entity.AccountingPeriod;
import fu.se2033.pharmacy.service.PeriodClosingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting-periods")
public class Flow4PeriodClosingController {
    private final PeriodClosingService periodClosingService;

    public Flow4PeriodClosingController(PeriodClosingService periodClosingService) {
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
