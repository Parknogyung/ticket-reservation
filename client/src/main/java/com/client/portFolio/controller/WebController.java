package com.client.portfolio.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Controller
public class WebController {

    @Value("${AUTH_SERVICE_URL:http://localhost:8082}")
    private String authServiceUrl;

    @Value("${TICKET_SERVER_URL:http://localhost:8081}")
    private String ticketServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/login")
    public String login(org.springframework.ui.Model model, java.security.Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("authServiceUrl", authServiceUrl);
        return "login";
    }

    @GetMapping("/register")
    public String register(org.springframework.ui.Model model, java.security.Principal principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
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
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("success", false, "message", e.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Register proxy error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/upload-image")
    @ResponseBody
    public ResponseEntity<Map> uploadImageProxy(@RequestParam("file") MultipartFile file) {
        try {
            String url = ticketServerUrl + "/api/upload-image";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Image upload proxy error: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("success", false, "message", "업로드 실패: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Image upload proxy error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "서버 오류가 발생했습니다."));
        }
    }
}
