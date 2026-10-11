package com.tunexa.config;

import com.tunexa.user.Role;

/** Utilisateur authentifié extrait du jeton JWT. */
public record AuthUser(Long id, Role role) {
}
