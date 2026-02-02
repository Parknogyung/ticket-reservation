package com.client.portfolio.controller;

import com.client.portfolio.service.AiServiceClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatController {

    private final AiServiceClient aiServiceClient;

    /**
     * 채팅 페이지 렌더링
     */
    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }

    /**
     * AI 채팅 API
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody ChatRequestDto request,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            String userId = authentication != null ? authentication.getName() : "anonymous";
            log.info("Chat request from user: {}, message: {}", userId, request.getMessage());

            String aiResponse = aiServiceClient.chat(userId, request.getMessage());

            response.put("success", true);
            response.put("message", aiResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chat request", e);

            response.put("success", false);
            response.put("error", "죄송합니다. 죄송합니다. AI 서비스 처리 중 오류가 발생했습니다.");

            return ResponseEntity.status(500).body(response);
        }
    }

    @Data
    public static class ChatRequestDto {
        private String message;
    }
}
