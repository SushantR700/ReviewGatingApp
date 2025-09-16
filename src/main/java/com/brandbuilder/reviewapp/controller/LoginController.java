package com.brandbuilder.reviewapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class LoginController {

    @GetMapping("/login/oauth2/authorization/google")
    public String googleLogin(@RequestParam(required = false) String role,
                              @RequestParam(required = false) String returnUrl,
                              HttpServletRequest request) {
        System.out.println("Login controller triggered with role: " + role + ", returnUrl: " + returnUrl);

        // Store the role and return URL in session to be used after OAuth callback
        HttpSession session = request.getSession();
        if (role != null) {
            session.setAttribute("login_role", role);
            System.out.println("Stored login_role in session: " + role);
        }

        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            session.setAttribute("return_url", returnUrl);
            System.out.println("Stored return_url in session: " + returnUrl);
        }

        // Redirect to the actual OAuth2 authorization endpoint
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/auth/google")
    public String googleAuth(@RequestParam(required = false) String role,
                             @RequestParam(required = false) String returnUrl,
                             HttpServletRequest request) {
        System.out.println("Alternative auth endpoint triggered with role: " + role + ", returnUrl: " + returnUrl);

        HttpSession session = request.getSession();
        if (role != null) {
            session.setAttribute("login_role", role);
            System.out.println("Stored login_role in session: " + role);
        }

        if (returnUrl != null && !returnUrl.trim().isEmpty()) {
            session.setAttribute("return_url", returnUrl);
            System.out.println("Stored return_url in session: " + returnUrl);
        }

        return "redirect:/oauth2/authorization/google";
    }
}