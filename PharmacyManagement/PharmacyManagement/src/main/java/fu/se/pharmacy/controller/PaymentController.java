package fu.se.pharmacy.controller;

import fu.se.pharmacy.dto.PaymentDTO;
import fu.se.pharmacy.dto.SaleDTO;
import fu.se.pharmacy.service.PaymentService;
import fu.se.pharmacy.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private SaleService saleService;

    /** Form thanh toán tiền mặt */
    @GetMapping("/cash/{saleId}")
    public String cashForm(@PathVariable Integer saleId, Model model) {
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
    public String processCash(@Valid @ModelAttribute("paymentDTO") PaymentDTO dto,
                              BindingResult result,
                              Model model,
                              RedirectAttributes ra) {
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

    /** Trang hóa đơn sau thanh toán */
    @GetMapping("/receipt/{saleId}")
    public String receipt(@PathVariable Integer saleId, Model model) {
        saleService.findById(saleId).ifPresent(s -> model.addAttribute("sale", s));
        paymentService.findBySaleId(saleId).ifPresent(p -> model.addAttribute("payment", p));
        return "payments/receipt";
    }

    /** Form thanh toán online / QR */
    @GetMapping("/online/{saleId}")
    public String onlineForm(@PathVariable Integer saleId, Model model) {
        SaleDTO sale = saleService.findById(saleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!"DRAFT".equals(sale.getStatus()))
            return "redirect:/sales/cart";
        PaymentDTO payment = paymentService.createOnlinePayment(saleId);
        model.addAttribute("sale", sale);
        model.addAttribute("payment", payment);
        return "payments/online";
    }
}