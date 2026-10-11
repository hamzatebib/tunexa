package com.tunexa.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.auth.AuthDtos.AuthResponse;
import com.tunexa.auth.AuthDtos.LoginRequest;
import com.tunexa.auth.AuthDtos.OtpRequest;
import com.tunexa.auth.AuthDtos.OtpResponse;
import com.tunexa.auth.AuthDtos.RegisterRequest;
import com.tunexa.auth.AuthDtos.ResetPasswordRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp")
    public OtpResponse requestOtp(@Valid @RequestBody OtpRequest req) {
        return authService.requestOtp(req.phone(), req.purpose());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req.phone(), req.password());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.phone(), req.code(), req.newPassword());
    }
}
