package com.example.kserverproject.domain.auth.controller;

import com.example.kserverproject.common.response.ApiResponse;
import com.example.kserverproject.domain.auth.dto.request.LoginRequestDto;
import com.example.kserverproject.domain.auth.dto.request.SignupRequestDto;
import com.example.kserverproject.domain.auth.dto.response.LoginResponseDto;
import com.example.kserverproject.domain.auth.dto.response.SignupResponseDto;
import com.example.kserverproject.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
