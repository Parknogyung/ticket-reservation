package com.ticket.portfolio.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.cloud.vertexai.VertexAI;
import java.io.IOException;

@Configuration
public class GeminiConfig {

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @Bean
    public VertexAI vertexAI() throws IOException {
        // 기본적으로 Application Default Credentials(ADC)를 사용합니다.
        // 환경 변수 GOOGLE_APPLICATION_CREDENTIALS가 설정되어 있어야 합니다.
        return new VertexAI(projectId, location);
    }

    @Bean
    public ChatModel chatModel(VertexAI vertexAI) {
        VertexAiGeminiChatOptions options = VertexAiGeminiChatOptions.builder()
                .model("gemini-1.5-flash")
                .temperature(0.7)
                .build();

        return new VertexAiGeminiChatModel(vertexAI, options);
    }
}
