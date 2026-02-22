package com.auth.controller;

import com.auth.entity.User;
import com.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:8083", "http://localhost:8080"}, allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login/success")
    public String loginSuccess(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "Login failed or not authenticated.";
        }
        Map<String, Object> attributes = principal.getAttributes();
        return "Login Success! User Attributes: " + attributes.toString();
    }

    @GetMapping("/")
    public String home() {
        return "Auth Service is running. Go to /login to sign in.";
    }
    
    @PostMapping("/api/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 유효성 검사
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "이메일은 필수입니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (request.getPassword() == null || request.getPassword().length() < 8) {
                response.put("success", false);
                response.put("message", "비밀번호는 8자 이상이어야 합니다.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // DB에 사용자 저장
            User user = userService.registerUser(
                request.getEmail(),
                request.getUsername(),
                request.getPassword()
            );
            
            log.info("User registered successfully: {}", user.getEmail());
            
            // 성공 응답
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("email", user.getEmail());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error during registration", e);
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/api/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // DB에서 사용자 검증
            boolean isValid = userService.validateUser(request.getEmail(), request.getPassword());
            
            if (isValid) {
                User user = userService.findByEmail(request.getEmail()).orElseThrow();
                
                response.put("success", true);
                response.put("message", "로그인 성공");
                response.put("email", user.getEmail());
                response.put("username", user.getUsername());
                
                log.info("User logged in successfully: {}", user.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during login", e);
            response.put("success", false);
            response.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    // DTO 클래스
    public static class RegisterRequest {
        private String email;
        private String username;
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class LoginRequest {
        private String email;
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
