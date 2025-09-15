package com.brandbuilder.reviewapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @GetMapping("/oauth2/authorization/google")
    public String googleLogin(@RequestParam(required = false) String role,
                              HttpServletRequest request) {
        // Store the role in session to be used after OAuth callback
        HttpSession session = request.getSession();
        if (role != null) {
            session.setAttribute("login_role", role);
        }

        // Redirect to the actual OAuth2 authorization endpoint
        return "redirect:/oauth2/authorization/google";
    }
}