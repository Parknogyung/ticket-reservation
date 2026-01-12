package com.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

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
}
