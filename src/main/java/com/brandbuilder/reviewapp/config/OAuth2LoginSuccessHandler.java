package com.brandbuilder.reviewapp.config;

import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import com.brandbuilder.reviewapp.service.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        HttpSession session = request.getSession();
        String loginRole = (String) session.getAttribute("login_role");

        // Get user info from OAuth2
        OAuth2User oauth2User = null;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            oauth2User = (OAuth2User) authentication.getPrincipal();
        }

        if (oauth2User != null) {
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String providerId = oauth2User.getAttribute("sub");

            // Find or create user
            Optional<User> existingUser = userRepository.findByProviderAndProviderId("GOOGLE", providerId);

            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setName(name);
                user.setEmail(email);
                user.setUpdatedAt(LocalDateTime.now());

                // Update role if admin login was requested
                if ("admin".equals(loginRole)) {
                    user.setRole(User.Role.ADMIN);
                }
            } else {
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setProvider("GOOGLE");
                user.setProviderId(providerId);
                user.setEnabled(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                // Set role based on login type
                if ("admin".equals(loginRole)) {
                    user.setRole(User.Role.ADMIN);
                } else {
                    user.setRole(User.Role.CUSTOMER);
                }
            }

            userRepository.save(user);

            // Clean up session
            session.removeAttribute("login_role");

            // Redirect based on role
            String targetUrl = "http://localhost:3000/";
            if (user.getRole() == User.Role.ADMIN) {
                targetUrl = "http://localhost:3000/admin";
            }

            response.sendRedirect(targetUrl);
        } else {
            response.sendRedirect("http://localhost:3000/?error=login_failed");
        }
    }
}