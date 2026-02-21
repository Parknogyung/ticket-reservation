package com.ticket.portfolio.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class OllamaChatService {

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2:latest}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaChatService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String message) {
        return chatWithContext(message, null);
    }

    public String chatWithContext(String message, String context) {
        try {
            String url = ollamaBaseUrl + "/api/chat";

            // Build messages array
            ArrayNode messages = objectMapper.createArrayNode();
            
            // System message with context
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            String systemPrompt = buildSystemPrompt(context);
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
            
            // User message
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", message);
            messages.add(userMessage);

            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.set("messages", messages);
            requestBody.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            log.info("Sending request to Ollama with model: {}", model);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.debug("Ollama API response: {}", responseBody);
                return extractMessageFromResponse(responseBody);
            } else {
                log.error("Ollama API error: {}", response.getStatusCode());
                return "AI 서비스 오류가 발생했습니다. (" + response.getStatusCode() + ")";
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to Ollama server", e);
            throw new RuntimeException("Ollama 서버에 연결할 수 없습니다. Ollama가 실행 중인지 확인해주세요. (http://localhost:11434)", e);
        } catch (Exception e) {
            log.error("Error calling Ollama API", e);
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt(String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 티켓 예매 시스템의 AI 어시스턴트입니다.\n");
        prompt.append("사용자의 질문에 친절하고 정확하게 답변해주세요.\n");
        prompt.append("티켓 예약, 공연 정보, 좌석 선택 등에 대해 도움을 드릴 수 있습니다.\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("\n=== 현재 시스템 정보 ===\n");
            prompt.append(context);
            prompt.append("\n===\n");
            prompt.append("위 정보를 바탕으로 사용자의 질문에 답변해주세요.\n");
        }
        
        return prompt.toString();
    }

    private String extractMessageFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            
            // Ollama chat API response format
            JsonNode messageNode = root.path("message");
            if (!messageNode.isMissingNode()) {
                String content = messageNode.path("content").asText();
                if (content != null && !content.isEmpty()) {
                    return content;
                }
            }
            
            // Fallback: try to get response directly
            String response = root.path("response").asText();
            if (response != null && !response.isEmpty()) {
                return response;
            }
            
            log.error("Unexpected Ollama response format: {}", responseBody);
            return "응답을 처리하는 중 오류가 발생했습니다.";
        } catch (Exception e) {
            log.error("Error parsing Ollama response: {}", responseBody, e);
            return "응답을 처리하는 중 오류가 발생했습니다.";
        }
    }
}
