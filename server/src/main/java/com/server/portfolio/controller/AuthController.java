package com.server.portfolio.controller;

import com.server.portfolio.domain.User;
import com.server.portfolio.repository.UserRepository;
import com.server.portfolio.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8083", "http://localhost:8080"}, allowCredentials = "true")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                response.put("success", false);
                response.put("message", "이미 등록된 이메일입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(User.Role.USER)
                    .point(0L)
                    .build();
            userRepository.save(user);

            log.info("User registered: {}", request.getEmail());
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Registration error", e);
            response.put("success", false);
            response.put("message", "회원가입 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElse(null);

            if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                response.put("success", false);
                response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다.");
                return ResponseEntity.status(401).body(response);
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), null,
                    java.util.Collections.singletonList(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));

            String accessToken = jwtTokenProvider.createAccessToken(authentication);
            String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

            redisTemplate.opsForValue().set(
                    "RT:" + loginRequest.getEmail(), refreshToken,
                    1209600000, java.util.concurrent.TimeUnit.MILLISECONDS);

            return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));

        } catch (Exception e) {
            log.error("Login error", e);
            response.put("success", false);
            response.put("message", "로그인 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest refreshRequest) {
        String oldRefreshToken = refreshRequest.getRefreshToken();

        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(oldRefreshToken)) {
            return ResponseEntity.status(401).body("만료되거나 유효하지 않은 리프레시 토큰입니다.");
        }

        // 2. Authentication 객체 추출
        Authentication authentication = jwtTokenProvider.getAuthentication(oldRefreshToken);
        String email = authentication.getName();

        // 3. Redis에서 토큰 일치 여부 확인 (Rotation 보장)
        String savedToken = redisTemplate.opsForValue().get("RT:" + email);
        if (savedToken == null || !savedToken.equals(oldRefreshToken)) {
            return ResponseEntity.status(401).body("잘못된 리프레시 요청입니다. 다시 로그인해주세요.");
        }

        // 4. 새 토큰 세트 생성 (Rotation!)
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // 5. Redis 갱신
        redisTemplate.opsForValue().set(
                "RT:" + email,
                newRefreshToken,
                1209600000,
                java.util.concurrent.TimeUnit.MILLISECONDS);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
    }

    @PostMapping("/init-test-data")
    public ResponseEntity<?> initTestData() {
        // This is a temporary method to replace the deleted DataInitializer
        // It's helpful for users to quickly set up seats for concurrency testing.
        log.info("Initializing test data requested via REST");
        // We'll call a service method or just return a message and implement it in
        // GrpcService if possible
        return ResponseEntity.ok("Use the gRPC service to init or add a dedicated method here.");
    }

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

    public static class RefreshRequest {
        private String refreshToken;

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;

        public TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
