package com.example.hockeyserver.security;

import com.example.hockeyserver.entity.Role;

public record AuthenticatedUser(
        Long id,
        String username,
        String email,
        Role role
) {
}
