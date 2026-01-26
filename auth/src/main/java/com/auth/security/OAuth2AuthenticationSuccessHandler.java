package com.auth.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email"); // Google
        if (email == null) {
            // Kakao returns attributes in a nested map, but Spring Security might flatten
            // it or we access via 'kakao_account'
            // For simplicity, let's try to find a unique ID
            Object kakaoAccount = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccount instanceof java.util.Map) {
                email = (String) ((java.util.Map) kakaoAccount).get("email");
            }
        }
        if (email == null) {
            email = String.valueOf(oAuth2User.getAttributes().get("id")); // Fallback to ID
        }

        log.info("OAuth2 Login Success. User Email/ID: {}", email);

        // Create an Authentication object with ROLE_USER
        UsernamePasswordAuthenticationToken authResult = new UsernamePasswordAuthenticationToken(
                email, null, Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));

        String accessToken = jwtTokenProvider.createAccessToken(authResult);
        String refreshToken = jwtTokenProvider.createRefreshToken(authResult);

        String clientPublicHost = System.getenv("CLIENT_PUBLIC_HOST");
        if (clientPublicHost == null || clientPublicHost.isEmpty()) {
            clientPublicHost = "localhost";
        }

        String targetUrl = UriComponentsBuilder.fromUriString("http://" + clientPublicHost + ":8083/auth/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        log.info("Redirecting to: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
