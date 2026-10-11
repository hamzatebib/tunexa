package com.tunexa.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.common.ApiException;
import com.tunexa.auth.AuthDtos.AuthResponse;
import com.tunexa.config.AuthUser;
import com.tunexa.config.JwtService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/me")
public class MeController {

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Email String email,
            @Pattern(regexp = "fr|ar|en") String language) {
    }

    private final UserRepository users;
    private final JwtService jwtService;

    public MeController(UserRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    /** Renvoie un jeton à jour (ex. après validation du compte vendeur, le rôle change). */
    @PostMapping("/token")
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(@AuthenticationPrincipal AuthUser auth) {
        User user = users.findById(auth.id()).filter(User::isEnabled)
                .orElseThrow(() -> ApiException.forbidden("Compte inactif"));
        return new AuthResponse(jwtService.issue(user), UserDto.of(user));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UserDto me(@AuthenticationPrincipal AuthUser auth) {
        return UserDto.of(users.findById(auth.id()).orElseThrow(() -> ApiException.notFound("Compte")));
    }

    @PutMapping
    @Transactional
    public UserDto update(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody UpdateProfileRequest req) {
        User user = users.findById(auth.id()).orElseThrow(() -> ApiException.notFound("Compte"));
        String email = req.email() == null || req.email().isBlank() ? null : req.email().trim().toLowerCase();
        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("email_taken", "Cet e-mail est déjà utilisé");
        }
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setEmail(email);
        if (req.language() != null) {
            user.setLanguage(req.language());
        }
        return UserDto.of(user);
    }
}
