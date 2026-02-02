package com.ticket.portfolio.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiChatService {

    private final ChatModel chatModel;

    /**
     * Gemini API를 호출하여 채팅 응답을 생성합니다.
     * 
     * @param userMessage 사용자 메시지
     * @return AI 응답 메시지
     */
    public String chat(String userMessage) {
        try {
            log.info("Sending message to Gemini: {}", userMessage);

            // ChatClient를 사용하여 간단한 메시지 전송
            ChatClient chatClient = ChatClient.create(chatModel);

            var chatResponse = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .chatResponse();

            if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
                log.error("Gemini API returned null response");
                throw new RuntimeException("Gemini API가 빈 응답을 반환했습니다.");
            }

            String response = chatResponse.getResult().getOutput().getContent();

            log.info("Received response from Gemini: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 대화 히스토리를 포함한 채팅
     * 
     * @param userMessage 사용자 메시지
     * @param history     대화 히스토리 (role, content 쌍)
     * @return AI 응답 메시지
     */
    public String chatWithHistory(String userMessage, List<com.ticket.portfolio.ChatMessage> history) {
        try {
            log.info("Sending message with history to Gemini");

            List<Message> messages = new ArrayList<>();

            // 히스토리를 Message로 변환
            if (history != null && !history.isEmpty()) {
                for (com.ticket.portfolio.ChatMessage msg : history) {
                    // Spring AI는 role이 "user"인 경우 UserMessage를 사용
                    if ("user".equals(msg.getRole())) {
                        messages.add(new UserMessage(msg.getContent()));
                    }
                    // assistant 메시지는 생략 (Gemini는 주로 user 메시지 기반)
                }
            }

            // 현재 메시지 추가
            messages.add(new UserMessage(userMessage));

            Prompt prompt = new Prompt(messages);
            var chatResponse = chatModel.call(prompt);
            
            if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
                log.error("Gemini API returned null response");
                throw new RuntimeException("Gemini API가 빈 응답을 반환했습니다.");
            }
            
            String response = chatResponse.getResult().getOutput().getContent();

            log.info("Received response from Gemini with history");
            return response;

        } catch (Exception e) {
            log.error("Error calling Gemini API with history", e);
            throw new RuntimeException("AI 서비스 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
