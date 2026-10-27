package com.shottrack.backend.application.auth;

import com.shottrack.backend.application.auth.dto.LoginRequest;
import com.shottrack.backend.application.auth.dto.LogoutRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetConfirmRequest;
import com.shottrack.backend.application.auth.dto.PasswordResetRequest;
import com.shottrack.backend.application.auth.dto.RefreshRequest;
import com.shottrack.backend.application.auth.dto.TokenResponse;
import com.shottrack.backend.application.auth.usecase.LoginService;
import com.shottrack.backend.application.auth.usecase.PasswordResetService;
import com.shottrack.backend.application.auth.usecase.RefreshTokenService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = loginService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = refreshTokenService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request);
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
