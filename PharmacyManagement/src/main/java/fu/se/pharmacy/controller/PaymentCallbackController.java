package fu.se.pharmacy.controller;

import fu.se.pharmacy.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentCallbackController {

    @Autowired private PaymentService paymentService;

    /**
     * Gateway gửi callback sau khi khách quét QR thành công.
     * Chỉ callback này mới được xác nhận PAID — Pharmacist không được tự làm.
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestBody Map<String, Object> payload) {
        try {
            Integer paymentId  = Integer.parseInt(payload.get("paymentId").toString());
            String  gatewayCode = payload.get("gatewayCode").toString();
            Integer amount     = Integer.parseInt(payload.get("amount").toString());
            String  status     = payload.get("status").toString();
            String  rawMsg     = payload.toString();

            if (!"SUCCESS".equals(status)) {
                return ResponseEntity.ok(Map.of("result", "IGNORED", "reason", "status not SUCCESS"));
            }

            paymentService.handleCallback(paymentId, gatewayCode, amount, rawMsg);
            return ResponseEntity.ok(Map.of("result", "OK"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
