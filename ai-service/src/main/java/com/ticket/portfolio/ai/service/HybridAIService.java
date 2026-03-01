package com.ticket.portfolio.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ollama와 AnythingLLM을 함께 사용하는 하이브리드 AI 서비스
 * - AnythingLLM: 문서 검색 및 RAG (Retrieval-Augmented Generation)
 * - Ollama: 실제 응답 생성
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridAIService {

    private final OllamaChatService ollamaChatService;
    private final GeminiChatService geminiChatService;
    private final AnythingLLMService anythingLLMService;
    private final ProjectDataService projectDataService;

    /**
     * 하이브리드 AI 채팅
     * 1. AnythingLLM에서 관련 문서 검색 (RAG)
     * 2. 프로젝트 데이터 가져오기
     * 3. Ollama로 응답 생성
     */
    public String chat(String message, String userContext) {
        StringBuilder enhancedContext = new StringBuilder();

        // 1. AnythingLLM에서 관련 문서 검색
        if (anythingLLMService.isAvailable()) {
            log.info("Searching relevant documents in AnythingLLM...");
            String ragContext = anythingLLMService.searchDocuments(message);

            if (ragContext != null && !ragContext.isEmpty()) {
                enhancedContext.append("=== 관련 문서 정보 (AnythingLLM) ===\n");
                enhancedContext.append(ragContext);
                enhancedContext.append("\n\n");
                log.info("Found relevant documents from AnythingLLM");
            } else {
                log.debug("No relevant documents found in AnythingLLM");
            }
        } else {
            log.debug("AnythingLLM is not available, skipping document search");
        }

        // 2. 프로젝트 실시간 데이터 가져오기
        try {
            String projectContext = projectDataService.getProjectContext();
            if (projectContext != null && !projectContext.isEmpty()) {
                enhancedContext.append("=== 실시간 시스템 정보 ===\n");
                enhancedContext.append(projectContext);
                enhancedContext.append("\n\n");
            }
        } catch (Exception e) {
            log.warn("Failed to get project context: {}", e.getMessage());
        }

        // 3. 사용자가 제공한 컨텍스트 추가
        if (userContext != null && !userContext.isEmpty()) {
            enhancedContext.append(userContext);
            enhancedContext.append("\n\n");
        }

        // 4. Ollama로 최종 응답 생성 (실패 시 Gemini로 fallback)
        String finalContext = enhancedContext.toString();
        log.info("Generating response with Ollama (context length: {} chars)", finalContext.length());

        try {
            return ollamaChatService.chatWithContext(message, finalContext);
        } catch (Exception ollamaEx) {
            log.warn("Ollama failed ({}), falling back to Gemini...", ollamaEx.getMessage());
            try {
                // Gemini는 context를 message에 포함시켜 전달
                String geminiMessage = finalContext.isEmpty()
                        ? message
                        : finalContext + "\n\n사용자 질문: " + message;
                return geminiChatService.chat(geminiMessage);
            } catch (Exception geminiEx) {
                log.error("Both Ollama and Gemini failed. Ollama: {}, Gemini: {}",
                        ollamaEx.getMessage(), geminiEx.getMessage());
                throw new RuntimeException("AI 서비스를 사용할 수 없습니다. Ollama: "
                        + ollamaEx.getMessage() + " / Gemini: " + geminiEx.getMessage());
            }
        }
    }

    /**
     * AnythingLLM만 사용하여 채팅 (Ollama 대신 AnythingLLM의 LLM 사용)
     */
    public String chatWithAnythingLLMOnly(String message) {
        if (!anythingLLMService.isAvailable()) {
            throw new RuntimeException("AnythingLLM 서버에 연결할 수 없습니다.");
        }

        return anythingLLMService.chat(message);
    }

    /**
     * Ollama만 사용하여 채팅 (문서 검색 없이)
     */
    public String chatWithOllamaOnly(String message, String context) {
        return ollamaChatService.chatWithContext(message, context);
    }

    /**
     * 시스템 상태 확인
     */
    public SystemStatus getStatus() {
        return SystemStatus.builder()
                .ollamaAvailable(true) // Ollama는 항상 사용 가능하다고 가정
                .anythingLLMAvailable(anythingLLMService.isAvailable())
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class SystemStatus {
        private boolean ollamaAvailable;
        private boolean anythingLLMAvailable;
    }
}
