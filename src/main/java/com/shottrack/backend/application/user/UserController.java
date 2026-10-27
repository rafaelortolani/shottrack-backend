package com.shottrack.backend.application.user;

import com.shottrack.backend.application.user.dto.ChangeEmailRequest;
import com.shottrack.backend.application.user.dto.ChangePasswordRequest;
import com.shottrack.backend.application.user.dto.CompleteRegistrationRequest;
import com.shottrack.backend.application.user.dto.DeleteAccountRequest;
import com.shottrack.backend.application.user.dto.ConfirmEmailChangeRequest;
import com.shottrack.backend.application.user.dto.RegistrationRequest;
import com.shottrack.backend.application.user.dto.UpdateProfileRequest;
import com.shottrack.backend.application.user.dto.UserDataExportResponse;
import com.shottrack.backend.application.user.dto.UserResponse;
import com.shottrack.backend.application.user.usecase.AccountDeletionService;
import com.shottrack.backend.application.user.usecase.UserDataExportService;
import com.shottrack.backend.application.user.usecase.UserService;
import com.shottrack.backend.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;
    private final UserDataExportService userDataExportService;

    @PostMapping("/registration")
    public ResponseEntity<ApiResponse<Void>> requestRegistration(@Valid @RequestBody RegistrationRequest request) {
        userService.requestRegistration(request);
        return ResponseEntity.accepted().body(ApiResponse.ok(null));
    }

    @PostMapping("/registration/completion")
    public ResponseEntity<ApiResponse<UserResponse>> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request) {
        UserResponse response = userService.completeRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(Authentication authentication,
                                                                      @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserResponse response = userService.updateProfile(userId, request);
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

    /**
     * UC53/ADR-0019: arquivo JSON para download — propositalmente sem o
     * envelope {"data": ...}, é o conteúdo que o atleta leva embora.
     */
    @GetMapping("/me/export")
    public ResponseEntity<UserDataExportResponse> exportData(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        UserDataExportResponse body = userDataExportService.export(userId);
        String filename = "shottrack-dados-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @PostMapping("/me/deletion")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(Authentication authentication,
                                                           @Valid @RequestBody DeleteAccountRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        accountDeletionService.delete(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(Authentication authentication,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
