package com.client.portFolio.controller;

import com.client.portFolio.security.UserPrincipal;
import com.ticket.portfolio.PaymentRequest;
import com.ticket.portfolio.PaymentResponse;
import com.ticket.portfolio.PaymentServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;

    @GetMapping("/payment")
    public String paymentPage(@RequestParam Long reservationId, @RequestParam Long seatId, Model model) {
        // In a real app, verify reservation belongs to user
        model.addAttribute("reservationId", reservationId);
        model.addAttribute("seatId", seatId);
        model.addAttribute("amount", 50000); // Fixed price for demo
        return "payment";
    }

    @PostMapping("/payment/process")
    public String processPayment(@RequestParam Long reservationId,
            @RequestParam Long amount,
            @RequestParam String paymentId,
            @AuthenticationPrincipal UserPrincipal user,
            Model model) {

        log.info("Processing payment: resId={}, amount={}, paymentId={}", reservationId, amount, paymentId);

        try {
            PaymentRequest request = PaymentRequest.newBuilder()
                    .setUserId(String.valueOf(user.getUserId()))
                    .setReservationId(reservationId)
                    .setAmount(amount)
                    .setPaymentId(paymentId)
                    .build();

            PaymentResponse response = paymentStub.processPayment(request);

            if (response.getSuccess()) {
                return "redirect:/dashboard?success=payment_complete";
            } else {
                model.addAttribute("error", response.getMessage());
                return "payment"; // Stay on page show error
            }
        } catch (Exception e) {
            log.error("Payment failed", e);
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다.");
            return "payment";
        }
    }
}
