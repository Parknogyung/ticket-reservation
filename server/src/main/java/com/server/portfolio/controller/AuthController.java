package com.server.portfolio.controller;

import com.server.portfolio.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 실제 운영 시에는 DB에서 사용자 확인 및 비밀번호 체크 로직이 들어가야 함
        // 현재는 학습 및 데모용으로 모든 로그인 허용 (테스트용 ID: user@example.com / PW: password)

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                null,
                java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        // Redis에 Refresh Token 저장 (Key: UserEmail, Value: RefreshToken, TTL: 14일)
        redisTemplate.opsForValue().set(
                "RT:" + loginRequest.getEmail(),
                refreshToken,
                1209600000,
                java.util.concurrent.TimeUnit.MILLISECONDS);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
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

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
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
