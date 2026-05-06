package com.example.kserverproject.domain.auth.controller;

import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.auth.dto.request.LoginRequestDto;
import com.example.kserverproject.domain.auth.dto.request.SignupRequestDto;
import com.example.kserverproject.domain.auth.dto.response.LoginResponseDto;
import com.example.kserverproject.domain.auth.dto.response.SignupResponseDto;
import com.example.kserverproject.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(authService.signup(requestDto)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.of(authService.login(requestDto)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String bearerToken) {

        String token = bearerToken.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
