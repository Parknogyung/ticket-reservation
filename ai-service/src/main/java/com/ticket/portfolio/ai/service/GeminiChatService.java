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
public class GeminiChatService {

    @Value("${spring.ai.google.genai.api-key}")
    private String apiKey;

    @Value("${spring.ai.google.genai.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiChatService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String message) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key="
                    + apiKey;

            // Build request body
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", message);
            parts.add(part);
            content.set("parts", parts);
            contents.add(content);
            requestBody.set("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.debug("Gemini API response: {}", responseBody);
                JsonNode responseJson = objectMapper.readTree(responseBody);
                return extractTextFromResponse(responseJson);
            } else {
                log.error("Gemini API error: {}", response.getStatusCode());
                return "AI 서비스 오류가 발생했습니다.";
            }
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.error("Gemini API rate limit exceeded", e);
            throw new RuntimeException("AI 서비스 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Gemini API HTTP error: {}", e.getStatusCode(), e);
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String extractTextFromResponse(JsonNode responseJson) {
        try {
            log.debug("Parsing response JSON: {}", responseJson.toString());
            
            JsonNode candidates = responseJson.path("candidates");
            if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
                log.error("No candidates found in response: {}", responseJson.toString());
                return "AI 응답을 찾을 수 없습니다.";
            }
            
            JsonNode firstCandidate = candidates.get(0);
            JsonNode content = firstCandidate.path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isMissingNode() || !parts.isArray() || parts.size() == 0) {
                log.error("No parts found in response content: {}", responseJson.toString());
                return "AI 응답 형식이 올바르지 않습니다.";
            }
            
            JsonNode firstPart = parts.get(0);
            JsonNode textNode = firstPart.path("text");
            
            if (textNode.isMissingNode()) {
                log.error("No text found in response part: {}", responseJson.toString());
                return "AI 응답 텍스트를 찾을 수 없습니다.";
            }
            
            return textNode.asText();
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", responseJson.toString(), e);
            return "응답을 처리하는 중 오류가 발생했습니다.";
        }
    }
}
