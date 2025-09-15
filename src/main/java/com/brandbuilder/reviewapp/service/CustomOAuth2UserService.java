package com.brandbuilder.reviewapp.service;

import com.brandbuilder.reviewapp.model.User;
import com.brandbuilder.reviewapp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        // Get role from request parameters (if available)
        String roleParam = userRequest.getAdditionalParameters().get("role") != null ?
                userRequest.getAdditionalParameters().get("role").toString() : null;

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update user info if needed
            user.setName(name);
            user.setEmail(email);
            user.setUpdatedAt(LocalDateTime.now());

            // Update role if specified and user doesn't already have admin role
            if (roleParam != null && "admin".equals(roleParam) && user.getRole() != User.Role.ADMIN) {
                user.setRole(User.Role.ADMIN);
            }
        } else {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProvider(provider.toUpperCase());
            user.setProviderId(providerId);

            // Set role based on login type or email domain
            if ("admin".equals(roleParam) || isAdminEmail(email)) {
                user.setRole(User.Role.ADMIN);
            } else {
                user.setRole(User.Role.CUSTOMER);
            }

            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
        }

        userRepository.save(user);

        return new CustomOAuth2User(oauth2User, user);
    }

    private boolean isAdminEmail(String email) {
        // You can define admin emails here
        // For now, we'll use a simple check
        return email != null && (
                email.equals("admin@brandbuilder.com") ||
                        email.endsWith("@brandbuilder.com") ||
                        email.equals("sushantregmi419@gmail.com") // Add your email for testing
        );
    }
}