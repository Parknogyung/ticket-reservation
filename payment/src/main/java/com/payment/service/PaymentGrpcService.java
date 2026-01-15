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

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        String userId = request.getUserId();
        long reservationId = request.getReservationId();
        long amount = request.getAmount();

        log.info("Processing payment: userId={}, reservationId={}, amount={}", userId, reservationId, amount);

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

        // TODO: Implement actual API call using iniApiKey and iniApiIv if needed for
        // server-side verification.
        // For PortOne V2, typically we verify using the paymentId and secret.
        // Since we have specific INIAPI keys, this might be a direct PG integration or
        // specific hybrid setup.

        if (request.getPaymentId() == null || request.getPaymentId().isEmpty()) {
            paymentSuccess = false;
        }

        if (paymentSuccess) {
            try {
                // 2. Call Ticket Server to complete reservation
                log.info("Payment successful. Completing reservation: {}", reservationId);

                CompleteReservationRequest completeRequest = CompleteReservationRequest.newBuilder()
                        .setUserId(userId)
                        .setReservationId(reservationId)
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
}
