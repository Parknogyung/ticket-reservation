package com.client.portfolio.service;

import com.ticket.portfolio.AIServiceGrpc;
import com.ticket.portfolio.ChatRequest;
import com.ticket.portfolio.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiServiceClient {

    @GrpcClient("ai-service")
    private AIServiceGrpc.AIServiceBlockingStub aiServiceStub;

    /**
     * AI 서비스에 메시지를 전송하고 응답을 받습니다.
     * 
     * @param userId  사용자 ID
     * @param message 사용자 메시지
     * @return AI 응답 메시지
     */
    public String chat(String userId, String message) {
        try {
            log.info("Sending chat request to AI service - userId: {}, message: {}", userId, message);

            ChatRequest request = ChatRequest.newBuilder()
                    .setUserId(userId)
                    .setMessage(message)
                    .build();

            ChatResponse response = aiServiceStub.chat(request);

            if (response.getSuccess()) {
                log.info("Received successful response from AI service");
                return response.getMessage();
            } else {
                log.error("AI service returned error: {}", response.getError());
                throw new RuntimeException("AI 서비스 오류: " + response.getError());
            }

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new RuntimeException("AI 서비스 호출 실패: " + e.getMessage(), e);
        }
    }
}
