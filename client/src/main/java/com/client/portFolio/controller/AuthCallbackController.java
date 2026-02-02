package com.client.portfolio.controller;

import com.client.portfolio.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;

@Slf4j
@Controller
public class AuthCallbackController {

    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String secretKey;

    private final com.client.portfolio.client.TicketServiceClient ticketServiceClient;

    public AuthCallbackController(com.client.portfolio.client.TicketServiceClient ticketServiceClient) {
        this.ticketServiceClient = ticketServiceClient;
    }

    @GetMapping("/auth/callback")
    public String authCallback(@RequestParam String accessToken,
            @RequestParam String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Auth callback received. Token: {}", accessToken.substring(0, 10) + "...");

        try {
            // 1. Parse JWT to get Email
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            String email = claims.getSubject();
            log.info("Extracted email from token: {}", email);

            // 2. Get Real User ID from Server
            com.ticket.portfolio.GetUserByEmailResponse userResponse = ticketServiceClient.getUserByEmail(email);
            long userId = userResponse.getUserId();
            log.info("Resolved userId: {}", userId);

            // 3. Create Principal with Real ID
            UserPrincipal principal = new UserPrincipal(userId, email, accessToken);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    accessToken,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Persist to session
            request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Error processing auth callback", e);
            return "redirect:/login?error=social_login_failed";
        }
    }
}
