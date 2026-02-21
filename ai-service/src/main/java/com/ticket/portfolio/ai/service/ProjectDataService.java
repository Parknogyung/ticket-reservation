package com.ticket.portfolio.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectDataService {

    @Value("${ticket.server.url:http://ticket-server:8081}")
    private String ticketServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getProjectContext() {
        StringBuilder context = new StringBuilder();
        
        context.append("티켓 예매 시스템 정보:\n");
        context.append("- 현재 시간: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // 공연 정보 가져오기
        try {
            Map<String, Object> response = restTemplate.getForObject(
                ticketServerUrl + "/api/concerts", 
                Map.class
            );
            
            if (response != null && response.containsKey("concerts")) {
                List<Map<String, Object>> concerts = (List<Map<String, Object>>) response.get("concerts");
                
                if (concerts == null || concerts.isEmpty()) {
                    context.append("현재 등록된 공연이 없습니다.\n");
                } else {
                    context.append("현재 등록된 공연 목록:\n");
                    for (Map<String, Object> concert : concerts) {
                        context.append("- ").append(concert.get("title")).append("\n");
                        context.append("  • 공연일: ").append(concert.get("concertDate")).append("\n");
                        context.append("  • 가격: ").append(String.format("%,d", concert.get("price"))).append("원\n");
                        context.append("  • 남은 좌석: ").append(concert.get("availableSeats")).append("석\n");
                        context.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch concert data: {}", e.getMessage());
            context.append("(공연 정보를 가져올 수 없습니다)\n");
        }
        
        context.append("\n사용 가능한 기능:\n");
        context.append("- 티켓 예약 및 결제\n");
        context.append("- 좌석 선택\n");
        context.append("- 예약 내역 조회\n");
        context.append("- 대기열 시스템을 통한 순차적 접근\n");
        
        return context.toString();
    }
}
