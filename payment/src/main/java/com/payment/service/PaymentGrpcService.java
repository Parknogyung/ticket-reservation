package com.payment.service;

import com.ticket.portfolio.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {

    @GrpcClient("ticket-server")
    private TicketServiceGrpc.TicketServiceBlockingStub ticketStub;

    @Override
    public void processPayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        String userId = request.getUserId();
        long reservationId = request.getReservationId();
        long amount = request.getAmount();

        log.info("Processing payment: userId={}, reservationId={}, amount={}", userId, reservationId, amount);

        // 1. Mock Payment Gateway Logic
        // In reality, we would call an external PG API here.
        boolean paymentSuccess = true;
        String transactionId = UUID.randomUUID().toString();

        if (amount <= 0) {
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
