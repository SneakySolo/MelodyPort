package com.melodyport.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {



    /**
     * Dashboard page after successful OAuth2 authentication.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            String displayName = principal.getAttribute("display_name");
            if (displayName == null) {
                displayName = principal.getAttribute("name");
            }
            if (displayName == null) {
                displayName = principal.getName();
            }

            model.addAttribute("name", displayName);
            model.addAttribute("email", principal.getAttribute("email"));

            java.util.Map<String, Object> attrs = principal.getAttributes();
            model.addAttribute("attributes", attrs != null ? attrs : new java.util.HashMap<>());
        }
        return "dashboard";
    }
}