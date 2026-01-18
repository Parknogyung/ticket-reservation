package com.payment.service;

import com.ticket.portfolio.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    @Value("${portone.channel-key}")
    private String channelKey;

    @Value("${portone.mid}")
    private String mid;

    @Value("${portone.sign-key}")
    private String signKey;

    @Value("${portone.iniapi-key}")
    private String iniApiKey;

    @Value("${portone.iniapi-iv}")
    private String iniApiIv;

    @Value("${portone.api-secret}")
    private String apiSecret;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    private static final String PORTONE_API_URL = "https://api.portone.io";

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        String userId = request.getUserId();
        java.util.List<Long> reservationIds = request.getReservationIdsList();
        long amount = request.getAmount();

        log.info("Processing payment: userId={}, reservationIds={}, amount={}", userId, reservationIds, amount);

        // 1. Verify Payment with PortOne (Mocking the API call using provided keys)
        // In a real production environment, you would use a RestTemplate or WebClient
        // to call PortOne/KG Inicis API.
        log.info("Verifying payment - ChannelKey: {}, MID: {}, PaymentId: {}", channelKey, mid, request.getPaymentId());

        // Simulating verification logic
        boolean paymentSuccess = true;
        String transactionId = UUID.randomUUID().toString();

        if (amount <= 0) {
            paymentSuccess = false;
        }

        if (request.getPaymentId() == null || request.getPaymentId().isEmpty()) {
            paymentSuccess = false;
        }

        if (paymentSuccess) {
            try {
                // 2. Call Ticket Server to complete reservation
                log.info("Payment successful. Completing reservations: {}", reservationIds);

                CompleteReservationRequest completeRequest = CompleteReservationRequest.newBuilder()
                        .setUserId(userId)
                        .addAllReservationIds(reservationIds)
                        .setPaymentId(request.getPaymentId())
                        .build();

                CompleteReservationResponse completeResponse = ticketStub.completeReservation(completeRequest);

                if (completeResponse.getSuccess()) {
                    PaymentResponse response = PaymentResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("결제 및 예약 확정이 완료되었습니다.")
                            .setTransactionId(transactionId)
                            .build();
                    responseObserver.onNext(response);
                } else {
                    // Payment succeeded but reservation completion failed (Need refund logic
                    // ideally)
                    log.error("Reservation completion failed: {}", completeResponse.getMessage());
                    PaymentResponse response = PaymentResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("결제는 성공했으나 예약 확정에 실패했습니다: " + completeResponse.getMessage())
                            .setTransactionId(transactionId)
                            .build();
                    responseObserver.onNext(response);
                }
            } catch (Exception e) {
                log.error("Error during reservation completion", e);
                PaymentResponse response = PaymentResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("예약 확정 중 오류가 발생했습니다.")
                        .build();
                responseObserver.onNext(response);
            }
        } else {
            PaymentResponse response = PaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("결제 승인에 실패했습니다.")
                    .build();
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void cancelPayment(CancelPaymentRequest request, StreamObserver<CancelPaymentResponse> responseObserver) {
        String paymentId = request.getPaymentId();
        java.util.List<Long> reservationIds = request.getReservationIdsList();
        String reason = request.getReason();

        log.info("Cancelling payment: paymentId={}, reservationIds={}, reason={}", paymentId, reservationIds, reason);

        // 1. Call PortOne to cancel payment
        boolean portOneCancelSuccess = false;
        try {
            String accessToken = getPortOneAccessToken();
            cancelPortOnePayment(paymentId, reason, accessToken);
            portOneCancelSuccess = true;
            log.info("PortOne cancel request successful.");
        } catch (Exception e) {
            log.error("PortOne cancel failed", e);
            CancelPaymentResponse response = CancelPaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("PG사 결제 취소 실패: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        if (portOneCancelSuccess) {
            try {
                // 2. Call Ticket Service to refund reservation (update status, free seat)
                RefundReservationRequest refundRequest = RefundReservationRequest.newBuilder()
                        .addAllReservationIds(reservationIds)
                        .build();

                RefundReservationResponse refundResponse = ticketStub.refundReservation(refundRequest);

                if (refundResponse.getSuccess()) {
                    CancelPaymentResponse response = CancelPaymentResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("결제 취소 및 환불이 완료되었습니다.")
                            .build();
                    responseObserver.onNext(response);
                } else {
                    log.error("Ticket refund failed: {}", refundResponse.getMessage());
                    CancelPaymentResponse response = CancelPaymentResponse.newBuilder()
                            .setSuccess(false) // Payment cancelled but ticket not? Inconsistency.
                            // In real world, we might need manual handling or retry.
                            .setMessage("결제는 취소되었으나 예약 환불 처리에 실패했습니다: " + refundResponse.getMessage())
                            .build();
                    responseObserver.onNext(response);
                }
            } catch (Exception e) {
                log.error("Error during refund reservation call", e);
                CancelPaymentResponse response = CancelPaymentResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("예약 환불 호출 중 오류 발생: " + e.getMessage())
                        .build();
                responseObserver.onNext(response);
            }
        } else {
            CancelPaymentResponse response = CancelPaymentResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("PG사 결제 취소 실패")
                    .build();
            responseObserver.onNext(response);
        }
        responseObserver.onCompleted();
    }

    private String getPortOneAccessToken() {
        String url = PORTONE_API_URL + "/login/api-secret";

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("apiSecret", apiSecret);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<java.util.Map<String, String>> entity = new org.springframework.http.HttpEntity<>(
                body, headers);

        try {
            // PortOne V2 login response structure
            // { "accessToken": "...", ... }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = restTemplate.postForObject(url, entity, java.util.Map.class);
            if (response != null && response.containsKey("accessToken")) {
                return (String) response.get("accessToken");
            }
            throw new RuntimeException("Refresh token failed or empty response");
        } catch (Exception e) {
            log.error("Failed to get PortOne access token", e);
            throw new RuntimeException("PortOne Login Failed", e);
        }
    }

    private void cancelPortOnePayment(String paymentId, String reason, String accessToken) {
        String url = PORTONE_API_URL + "/payments/" + paymentId + "/cancel";

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("reason", reason);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);

        org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(
                body, headers);

        // Call Cancel API
        restTemplate.postForObject(url, entity, String.class);
    }
}
