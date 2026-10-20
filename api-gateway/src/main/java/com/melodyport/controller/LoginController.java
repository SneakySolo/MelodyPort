package com.melodyport.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * Login page with Spotify OAuth button.
     * Redirects to auth-service (8081) for actual OAuth flow.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Dashboard page after successful OAuth2 authentication.
     */
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}