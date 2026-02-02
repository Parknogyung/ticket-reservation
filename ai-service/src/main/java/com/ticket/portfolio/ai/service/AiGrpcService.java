package com.ticket.portfolio.ai.service;

import com.ticket.portfolio.AIServiceGrpc;
import com.ticket.portfolio.ChatRequest;
import com.ticket.portfolio.ChatResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class AiGrpcService extends AIServiceGrpc.AIServiceImplBase {

    private final GeminiChatService geminiChatService;

    @Override
    public void chat(ChatRequest request, StreamObserver<ChatResponse> responseObserver) {
        log.info("Received chat request from user: {}, message: {}",
                request.getUserId(), request.getMessage());

        try {
            // Simple chat without history for now
            String aiResponse = geminiChatService.chat(request.getMessage());

            // 성공 응답 생성
            ChatResponse response = ChatResponse.newBuilder()
                    .setMessage(aiResponse)
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            log.info("Successfully sent AI response to user: {}", request.getUserId());

        } catch (Exception e) {
            log.error("Error processing chat request", e);

            // 에러 응답 생성
            ChatResponse errorResponse = ChatResponse.newBuilder()
                    .setMessage("죄송합니다. 현재 AI 서비스에 문제가 발생했습니다.")
                    .setSuccess(false)
                    .setError(e.getMessage())
                    .build();

            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
}
