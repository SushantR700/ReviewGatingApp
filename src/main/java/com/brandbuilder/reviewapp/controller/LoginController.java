package com.brandbuilder.reviewapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @GetMapping("/login/oauth2/authorization/google")
    public String googleLogin(@RequestParam(required = false) String role,
                              HttpServletRequest request) {
        System.out.println("Login controller triggered with role: " + role);

        // Store the role in session to be used after OAuth callback
        HttpSession session = request.getSession();
        if (role != null) {
            session.setAttribute("login_role", role);
            System.out.println("Stored login_role in session: " + role);
        }

        // Redirect to the actual OAuth2 authorization endpoint
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/auth/google")
    public String googleAuth(@RequestParam(required = false) String role,
                             HttpServletRequest request) {
        System.out.println("Alternative auth endpoint triggered with role: " + role);

        HttpSession session = request.getSession();
        if (role != null) {
            session.setAttribute("login_role", role);
            System.out.println("Stored login_role in session: " + role);
        }

        return "redirect:/oauth2/authorization/google";
    }
}