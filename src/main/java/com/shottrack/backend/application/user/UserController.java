package com.shottrack.backend.application.user;

import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ChangePasswordRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.UpdateNameRequest;
import com.shottrack.backend.application.user.dto.UserRegisterRequest;
import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.usecase.UserService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateName(Authentication authentication,
                                                                  @Valid @RequestBody UpdateNameRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.updateName(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/me/email")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(Authentication authentication,
                                                                  @Valid @RequestBody ChangeEmailRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.requestEmailChange(userId, request);
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }

    @PostMapping("/me/email/confirmation")
    public ResponseEntity<ApiResponse<UserResponse>> confirmEmailChange(Authentication authentication,
                                                                          @Valid @RequestBody ConfirmEmailChangeRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.confirmEmailChange(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
