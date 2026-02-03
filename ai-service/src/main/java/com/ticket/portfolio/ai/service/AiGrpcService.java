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
            String enhancedMessage = buildEnhancedMessage(request);
            String aiResponse = geminiChatService.chat(enhancedMessage);

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

    private String buildEnhancedMessage(ChatRequest request) {
        StringBuilder messageBuilder = new StringBuilder();
        
        // 시스템 프롬프트 추가
        messageBuilder.append("당신은 티켓 예매 시스템의 AI 어시스턴트입니다. ");
        messageBuilder.append("사용자의 질문에 친절하고 정확하게 답변해주세요.\n\n");
        
        // 대화 히스토리가 있으면 추가
        if (request.getHistoryCount() > 0) {
            messageBuilder.append("=== 이전 대화 ===\n");
            for (var historyMsg : request.getHistoryList()) {
                String role = historyMsg.getRole().equals("user") ? "사용자" : "AI";
                messageBuilder.append(role).append(": ").append(historyMsg.getContent()).append("\n");
            }
            messageBuilder.append("\n");
        }
        
        // 현재 사용자 메시지
        messageBuilder.append("=== 현재 질문 ===\n");
        messageBuilder.append(request.getMessage());
        
        return messageBuilder.toString();
    }
}
