package com.client.portFolio.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class WebController {

    @org.springframework.beans.factory.annotation.Value("${AUTH_SERVICE_URL:http://localhost:8082}")
    private String authServiceUrl;

    @GetMapping("/login")
    public String login(org.springframework.ui.Model model) {
        model.addAttribute("authServiceUrl", authServiceUrl);
        return "login";
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
}
