package fu.se.pharmacy.controller;

import fu.se.pharmacy.config.AuthInterceptor;
import fu.se.pharmacy.dto.PaymentDTO;
import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.service.PaymentService;
import fu.se.pharmacy.service.SaleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Phân quyền theo tài liệu nghiệp vụ (Người 3 - Thanh toán):
 * "Pharmacist chọn CASH/ONLINE... Pharmacist xác nhận đã nhận tiền."
 * "Admin không duyệt từng hóa đơn tiền mặt."
 * → CHỈ Pharmacist được thực hiện thanh toán (cash/online).
 * Trang receipt (xem hóa đơn sau thanh toán) cho phép mọi role đã đăng nhập xem.
 */
@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private SaleService saleService;

    /** Form thanh toán tiền mặt */
    @GetMapping("/cash/{saleId}")
    public String cashForm(@PathVariable Integer saleId, HttpSession session, Model model) {
        // FIX: chỉ Pharmacist được thanh toán
        AuthInterceptor.requireRole(session, "Pharmacist");
        SaleDTO sale = saleService.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"DRAFT".equals(sale.getStatus()))
            return "redirect:/sales/cart";
        PaymentDTO dto = new PaymentDTO();
        dto.setSaleId(saleId);
        model.addAttribute("sale", sale);
        model.addAttribute("paymentDTO", dto);
        return "payments/cash";
    }

    /** Xử lý thanh toán tiền mặt */
    @PostMapping("/cash")
    public String processCash(HttpSession session,
                              @Valid @ModelAttribute("paymentDTO") PaymentDTO dto,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
        AuthInterceptor.requireRole(session, "Pharmacist");
        if (result.hasErrors()) {
            saleService.findById(dto.getSaleId()).ifPresent(s -> model.addAttribute("sale", s));
            return "payments/cash";
        }
        try {
            PaymentDTO payment = paymentService.processCash(dto);
            ra.addFlashAttribute("payment", payment);
            ra.addFlashAttribute("success",
                    "Thanh toán thành công! Tiền thừa: "
                            + String.format("%,d", payment.getChangeAmount()) + " đ");
            return "redirect:/payments/receipt/" + dto.getSaleId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/payments/cash/" + dto.getSaleId();
        }
    }

    /** Trang hóa đơn sau thanh toán - mọi role đã đăng nhập đều xem được */
    @GetMapping("/receipt/{saleId}")
    public String receipt(@PathVariable Integer saleId, HttpSession session, Model model) {
        AuthInterceptor.requireLogin(session);
        saleService.findById(saleId).ifPresent(s -> model.addAttribute("sale", s));
        paymentService.findBySaleId(saleId).ifPresent(p -> model.addAttribute("payment", p));
        return "payments/receipt";
    }

    /** Form thanh toán online / QR */
    @GetMapping("/online/{saleId}")
    public String onlineForm(@PathVariable Integer saleId, HttpSession session, Model model) {
        // FIX: chỉ Pharmacist được tạo thanh toán online
        AuthInterceptor.requireRole(session, "Pharmacist");
        SaleDTO sale = saleService.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"DRAFT".equals(sale.getStatus()))
            return "redirect:/sales/cart";
        PaymentDTO payment = paymentService.createOnlinePayment(saleId);
        model.addAttribute("sale", sale);
        model.addAttribute("payment", payment);
        return "payments/online";
    }

    /**
     * API polling: trang online.html gọi mỗi 5 giây để tự động redirect
     * khi gateway xác nhận thanh toán thành công.
     * Trả JSON thay vì HTML để JS xử lý được.
     */
    @GetMapping("/check-status/{saleId}")
    @ResponseBody
    public java.util.Map<String, Object> checkStatus(
            @PathVariable Integer saleId, HttpSession session) {
        AuthInterceptor.requireLogin(session);
        return paymentService.findBySaleId(saleId)
                .map(p -> java.util.Map.<String, Object>of(
                        "status", p.getStatus() != null ? p.getStatus() : "PENDING",
                        "paymentId", p.getPaymentId() != null ? p.getPaymentId() : 0))
                .orElse(java.util.Map.of("status", "NOT_FOUND"));
    }
}