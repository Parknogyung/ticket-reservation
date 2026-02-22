package com.client.portfolio.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Controller
public class WebController {

    @Value("${AUTH_SERVICE_URL:http://localhost:8082}")
    private String authServiceUrl;

    @Value("${TICKET_SERVER_URL:http://ticket-server:8081}")
    private String ticketServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/login")
    public String login(org.springframework.ui.Model model) {
        model.addAttribute("authServiceUrl", authServiceUrl);
        return "login";
    }

    @GetMapping("/register")
    public String register(org.springframework.ui.Model model) {
        model.addAttribute("authServiceUrl", authServiceUrl);
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        log.info("Accessing dashboard page");
        return "dashboard";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @PostMapping("/api/register")
    @ResponseBody
    public ResponseEntity<Map> registerProxy(@RequestBody Map<String, String> request) {
        try {
            String url = ticketServerUrl + "/api/auth/register";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Register proxy error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("success", false, "message", e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Register proxy error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
}
