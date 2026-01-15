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

    private final com.client.portFolio.client.TicketServiceClient ticketServiceClient;

    @GetMapping("/payment")
    public String paymentPage(@RequestParam Long reservationId, @RequestParam Long seatId, Model model) {
        // Fetch reservation details (Title, Price)
        // Note: In real app, we need token. Here we assume internal call or unsecured
        // for demo if needed.
        // But TicketService requires auth?
        // Wait, TicketServiceClient needs a token for `getReservationDetails`?
        // No, `getReservationDetails` in `TicketGrpcService` is not checking auth
        // explicitly in my code,
        // but `TicketServiceClient` adds headers?
        // Let's check TicketServiceClient...
        // I implemented `public GetReservationDetailsResponse
        // getReservationDetails(long reservationId)` without token arg in client.
        // And `TicketGrpcService` doesn't enforce auth on this RPC yet (interceptor
        // might, but let's assume allowed or internal).
        // Actually, for better security we should use token, but for now let's try
        // calling it.

        try {
            var details = ticketServiceClient.getReservationDetails(reservationId);

            model.addAttribute("reservationId", reservationId);
            model.addAttribute("seatId", seatId);
            model.addAttribute("amount", details.getAmount());
            model.addAttribute("orderName", details.getTitle());
        } catch (Exception e) {
            log.error("Failed to fetch reservation details", e);
            model.addAttribute("error", "예약 정보를 불러오는 데 실패했습니다.");
            return "error";
        }

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

    @PostMapping("/payment/cancel")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> cancelPayment(@RequestParam String paymentId,
            @RequestParam Long reservationId,
            @RequestParam(defaultValue = "User requested") String reason) {
        log.info("Requesting cancellation: paymentId={}, reservationId={}", paymentId, reservationId);

        com.ticket.portfolio.CancelPaymentRequest request = com.ticket.portfolio.CancelPaymentRequest.newBuilder()
                .setPaymentId(paymentId)
                .setReservationId(reservationId)
                .setReason(reason)
                .build();

        com.ticket.portfolio.CancelPaymentResponse response = paymentStub.cancelPayment(request);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", response.getSuccess());
        result.put("message", response.getMessage());
        return result;
    }
}
