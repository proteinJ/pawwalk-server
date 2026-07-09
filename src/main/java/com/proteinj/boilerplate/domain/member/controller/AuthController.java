package com.proteinj.boilerplate.domain.member.controller;

import com.proteinj.boilerplate.domain.member.domain.TokenDto;
import com.proteinj.boilerplate.domain.member.dto.LoginRequest;
import com.proteinj.boilerplate.domain.member.dto.RefreshRequest;
import com.proteinj.boilerplate.domain.member.dto.SignupRequest;
import com.proteinj.boilerplate.domain.member.service.AuthService;
import com.proteinj.boilerplate.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입 완료"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenDto>> login(@Valid @RequestBody LoginRequest request) {
        TokenDto tokenDto = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", tokenDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenDto>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenDto tokenDto = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", tokenDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody RefreshRequest request) {
        String accessToken = authorizationHeader.replace("Bearer ", "");
        authService.logout(accessToken, request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃 완료"));
    }
}
