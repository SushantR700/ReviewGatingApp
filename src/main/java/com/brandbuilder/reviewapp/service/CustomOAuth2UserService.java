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
        System.out.println("=== CustomOAuth2UserService.loadUser ===");

        OAuth2User oauth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String providerId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        System.out.println("OAuth2 User Info:");
        System.out.println("- Provider: " + provider);
        System.out.println("- Provider ID: " + providerId);
        System.out.println("- Email: " + email);
        System.out.println("- Name: " + name);

        // Check if user already exists by provider and providerId
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            System.out.println("Existing user found:");
            System.out.println("- User ID: " + user.getId());
            System.out.println("- Current Role: " + user.getRole());

            // Update user info if needed
            boolean updated = false;
            if (!name.equals(user.getName())) {
                user.setName(name);
                updated = true;
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }

            if (updated) {
                user.setUpdatedAt(LocalDateTime.now());
                user = userRepository.save(user);
                System.out.println("User info updated");
            }
        } else {
            System.out.println("Creating new user");

            // Create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Set role based on email domain or specific emails
            if (isAdminEmail(email)) {
                user.setRole(User.Role.ADMIN);
                System.out.println("New user created with ADMIN role (based on email)");
            } else {
                user.setRole(User.Role.CUSTOMER);
                System.out.println("New user created with CUSTOMER role");
            }

            user = userRepository.save(user);
            System.out.println("New user saved with ID: " + user.getId());
        }

        System.out.println("Final user role: " + user.getRole());
        return new CustomOAuth2User(oauth2User, user);
    }

    private boolean isAdminEmail(String email) {
        if (email == null) return false;

        boolean isAdmin = email.equals("sushantregmi419@gmail.com") ||
                email.endsWith("@brandbuilder.com");

        System.out.println("Admin email check for '" + email + "': " + isAdmin);
        return isAdmin;
    }
}