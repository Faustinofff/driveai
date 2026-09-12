package com.driveai.dto.auth;

import com.driveai.domain.enums.UserRole;
import com.driveai.domain.model.AppUser;

public record AuthResponse(
        String token,
        UserResponse user) {

    public static AuthResponse of(AppUser user, String token) {
        return new AuthResponse(token, UserResponse.from(user));
    }

    public record UserResponse(Long id, String email, String fullName, UserRole role) {
        public static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        }
    }
}