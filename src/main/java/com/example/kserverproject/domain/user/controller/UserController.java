package com.example.kserverproject.domain.user.controller;

import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.common.dto.response.ApiResponse;
import com.example.kserverproject.domain.user.dto.response.UserInformationResponseDto;
import com.example.kserverproject.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInformationResponseDto>> getUserInformation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponse.of(userService.getUserInformation(userId)));
    }
}
