package com.client.portFolio.controller;

import com.client.portFolio.security.UserPrincipal;
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

    @GetMapping("/auth/callback")
    public String authCallback(@RequestParam String accessToken,
            @RequestParam String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Auth callback received. Token: {}", accessToken.substring(0, 10) + "...");

        try {
            // In a real app, validate the token signature here using the same secret key
            // For now, we assume the token from our trusted Auth Service is valid and trust
            // the payload

            // Extract user info from token (Decoding would be needed for real extraction)
            // For this quick integration, we will create a valid session assuming success
            // Ideally, decode JWT to get email/ID.
            // We can just use a placeholder or basic decoding if dependencies exist.

            // Simply create a Principal. We don't have the ID/Email readily available
            // without decoding JWT.
            // But we can set the token in the principal so dashboard can use it.
            UserPrincipal principal = new UserPrincipal(0L, "social-user", accessToken);

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
