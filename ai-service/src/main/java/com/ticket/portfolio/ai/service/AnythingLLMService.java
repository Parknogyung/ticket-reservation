package com.ticket.portfolio.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class AnythingLLMService {

    @Value("${anythingllm.base-url:http://localhost:3001}")
    private String anythingLLMBaseUrl;

    @Value("${anythingllm.api-key:}")
    private String apiKey;

    @Value("${anythingllm.workspace-slug:ticket-system}")
    private String workspaceSlug;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AnythingLLMService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * AnythingLLM에서 관련 문서를 검색합니다 (RAG)
     */
    public String searchDocuments(String query) {
        try {
            String url = anythingLLMBaseUrl + "/api/v1/workspace/" + workspaceSlug + "/chat";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("message", query);
            requestBody.put("mode", "query"); // RAG 모드

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            log.info("Searching documents in AnythingLLM for query: {}", query);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractRelevantContext(response.getBody());
            } else {
                log.warn("AnythingLLM search failed with status: {}", response.getStatusCode());
                return null;
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Cannot connect to AnythingLLM server: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("AnythingLLM search skipped: {}", e.getMessage());
            return null;
        }
    }

    /**
     * AnythingLLM에서 채팅 (문서 검색 + LLM 응답)
     */
    public String chat(String message) {
        try {
            String url = anythingLLMBaseUrl + "/api/v1/workspace/" + workspaceSlug + "/chat";

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("message", message);
            requestBody.put("mode", "chat"); // 채팅 모드

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            log.info("Sending chat request to AnythingLLM: {}", message);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractChatResponse(response.getBody());
            } else {
                log.error("AnythingLLM chat failed with status: {}", response.getStatusCode());
                return null;
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.warn("Cannot connect to AnythingLLM server: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error calling AnythingLLM chat", e);
            return null;
        }
    }

    private String extractRelevantContext(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // AnythingLLM의 sources 필드에서 관련 문서 추출
            JsonNode sources = root.path("sources");
            if (!sources.isMissingNode() && sources.isArray()) {
                StringBuilder context = new StringBuilder();
                for (JsonNode source : sources) {
                    String text = source.path("text").asText();
                    if (text != null && !text.isEmpty()) {
                        context.append(text).append("\n\n");
                    }
                }
                return context.toString().trim();
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting context from AnythingLLM response", e);
            return null;
        }
    }

    private String extractChatResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // AnythingLLM의 textResponse 필드에서 응답 추출
            String textResponse = root.path("textResponse").asText();
            if (textResponse != null && !textResponse.isEmpty()) {
                return textResponse;
            }

            log.error("No textResponse found in AnythingLLM response: {}", responseBody);
            return null;
        } catch (Exception e) {
            log.error("Error extracting chat response from AnythingLLM", e);
            return null;
        }
    }

    /**
     * AnythingLLM 연결 상태 확인
     */
    public boolean isAvailable() {
        try {
            String url = anythingLLMBaseUrl + "/api/ping";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("AnythingLLM is not available: {}", e.getMessage());
            return false;
        }
    }
}
