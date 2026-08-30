package com.lawfirm.auth;

import com.lawfirm.auth.dto.ChangePasswordRequest;
import com.lawfirm.auth.dto.LoginRequest;
import com.lawfirm.auth.dto.LoginResponse;
import com.lawfirm.auth.dto.ProfileRequest;
import com.lawfirm.auth.dto.UserInfo;
import com.lawfirm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfo> me() {
        return ApiResponse.ok(authService.me());
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }

    @PutMapping("/profile")
    public ApiResponse<UserInfo> updateProfile(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(authService.updateProfile(request));
    }

    @PostMapping("/avatar")
    public ApiResponse<UserInfo> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(authService.uploadAvatar(file));
    }
}
