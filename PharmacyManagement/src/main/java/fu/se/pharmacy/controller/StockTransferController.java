package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.ApprovalRequest;
import fu.se.pharmacy.dto.StockTransferCreateRequest;
import fu.se.pharmacy.dto.StockTransferReceiveRequest;
import fu.se.pharmacy.dto.StockTransferSendRequest;
import fu.se.pharmacy.entity.StockTransfer;
import fu.se.pharmacy.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
public class StockTransferController {
    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    public List<StockTransfer> getAll(@RequestParam(required = false) Integer branchId) {
        if (branchId != null) {
            return stockTransferService.getByBranch(branchId);
        }
        return stockTransferService.getAll();
    }

    @GetMapping("/{id}")
    public StockTransfer getById(@PathVariable Integer id) {
        return stockTransferService.getById(id);
    }

    @PostMapping
    public StockTransfer create(@Valid @RequestBody StockTransferCreateRequest request) {
        return stockTransferService.createTransfer(request);
    }

    @PostMapping("/{id}/approve")
    public StockTransfer approve(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request) {
        return stockTransferService.approveTransfer(id, request);
    }

    @PostMapping("/{id}/reject")
    public StockTransfer reject(@PathVariable Integer id, @Valid @RequestBody ApprovalRequest request) {
        return stockTransferService.rejectTransfer(id, request);
    }

    @PostMapping("/{id}/send")
    public StockTransfer confirmSent(@PathVariable Integer id, @Valid @RequestBody StockTransferSendRequest request) {
        return stockTransferService.confirmSent(id, request);
    }

    @PostMapping("/{id}/receive")
    public StockTransfer confirmReceived(@PathVariable Integer id, @Valid @RequestBody StockTransferReceiveRequest request) {
        return stockTransferService.confirmReceived(id, request);
    }

    @PostMapping("/{id}/cancel")
    public StockTransfer cancel(@PathVariable Integer id,
                                @RequestParam Integer userId,
                                @RequestParam(required = false) String reason) {
        return stockTransferService.cancelTransfer(id, userId, reason);
    }
}
