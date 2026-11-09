package com.example.itsm.user;

public record AuthResponse(String token, String tokenType, UserResponse user) {

    public static AuthResponse bearer(String token, User user) {
        return new AuthResponse(token, "Bearer", UserResponse.from(user));
    }
}
