package com.brandbuilder.reviewapp.controller;

import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthController {

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("authenticated", true);
        userInfo.put("id", oauth2User.getUserId());
        userInfo.put("email", oauth2User.getEmail());
        userInfo.put("name", oauth2User.getName());
        userInfo.put("role", oauth2User.getRole().name());

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(Authentication authentication) {
        Map<String, Object> status = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            status.put("authenticated", true);
            status.put("role", oauth2User.getRole().name());
        } else {
            status.put("authenticated", false);
            status.put("role", null);
        }

        return ResponseEntity.ok(status);
    }
}