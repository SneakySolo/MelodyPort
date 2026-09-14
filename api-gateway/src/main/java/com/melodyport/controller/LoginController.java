package com.melodyport.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    /**
     * Home page - redirects to OAuth2 authorization if not authenticated.
     * Spring Security will auto-handle this and show default login page.
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            model.addAttribute("name", principal.getName());
            return "dashboard";
        }
        return "redirect:/oauth2/authorization/spotify";
    }

    /**
     * Dashboard - only accessible when authenticated
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            model.addAttribute("name", principal.getAttribute("display_name") != null
                    ? principal.getAttribute("display_name")
                    : principal.getName());
            model.addAttribute("email", principal.getAttribute("email"));
            model.addAttribute("attributes", principal.getAttributes());
        }
        return "dashboard";
    }
}