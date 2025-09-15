package com.brandbuilder.reviewapp.config;

import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
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

        System.out.println("OAuth2 Login Success Handler triggered");

        HttpSession session = request.getSession();
        String loginRole = (String) session.getAttribute("login_role");
        System.out.println("Login role from session: " + loginRole);

        // Get user info from OAuth2
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub");

        System.out.println("OAuth2 user info - Email: " + email + ", Name: " + name + ", ProviderId: " + providerId);

        if (oauth2User != null && email != null) {
            // Find or create user
            Optional<User> existingUser = userRepository.findByProviderAndProviderId("GOOGLE", providerId);

            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setName(name);
                user.setEmail(email);
                user.setUpdatedAt(LocalDateTime.now());

                System.out.println("Existing user found - Current role: " + user.getRole());

                // Update role if admin login was requested and user is not already admin
                if ("admin".equals(loginRole)) {
                    user.setRole(User.Role.ADMIN);
                    System.out.println("Updated user role to ADMIN");
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

                // Set role based on login type or email
                if ("admin".equals(loginRole) || isAdminEmail(email)) {
                    user.setRole(User.Role.ADMIN);
                    System.out.println("New user created with ADMIN role");
                } else {
                    user.setRole(User.Role.CUSTOMER);
                    System.out.println("New user created with CUSTOMER role");
                }
            }

            // Save user
            User savedUser = userRepository.save(user);
            System.out.println("User saved with role: " + savedUser.getRole());

            // Clean up session
            session.removeAttribute("login_role");

            // Redirect based on role
            String targetUrl = "http://localhost:3000/";
            if (savedUser.getRole() == User.Role.ADMIN) {
                targetUrl = "http://localhost:3000/admin";
            }

            System.out.println("Redirecting to: " + targetUrl);
            response.sendRedirect(targetUrl);
        } else {
            System.out.println("OAuth2 user data incomplete, redirecting with error");
            response.sendRedirect("http://localhost:3000/?error=login_failed");
        }
    }

    private boolean isAdminEmail(String email) {
        return email != null && (
                email.equals("sushantregmi419@gmail.com") ||
                        email.endsWith("@brandbuilder.com")
        );
    }
}