package com.tunexa.user;

public record UserDto(Long id, String phone, String email, String firstName, String lastName, String language, Role role) {

    public static UserDto of(User u) {
        return new UserDto(u.getId(), u.getPhone(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getLanguage(),
                u.getRole());
    }
}
