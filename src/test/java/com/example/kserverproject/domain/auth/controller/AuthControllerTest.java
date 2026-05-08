package com.example.kserverproject.domain.auth.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.auth.dto.request.LoginRequestDto;
import com.example.kserverproject.domain.auth.dto.request.SignupRequestDto;
import com.example.kserverproject.domain.auth.dto.response.LoginResponseDto;
import com.example.kserverproject.domain.auth.dto.response.SignupResponseDto;
import com.example.kserverproject.domain.auth.service.AuthService;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/auth/signup - 회원가입")
    class Signup {

        @Test
        @DisplayName("정상 요청 시 201과 유저 정보를 반환한다")
        void signup_success() throws Exception {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("user1@test.com")
                    .password("password123!")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            SignupResponseDto response = new SignupResponseDto(2L, "user1@test.com", "테스터1", UserRole.CUSTOMER);
            given(authService.signup(any())).willReturn(response);

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("user1@test.com"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
        void signup_invalidEmail_returns400() throws Exception {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("invalid-email")
                    .password("password123!")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 400을 반환한다")
        void signup_shortPassword_returns400() throws Exception {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("user1@test.com")
                    .password("short")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("중복 이메일이면 409를 반환한다")
        void signup_duplicateEmail_returns400() throws Exception {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("user1@test.com")
                    .password("password123!")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            given(authService.signup(any())).willThrow(new UserException(ErrorCode.DUPLICATE_EMAIL));

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login - 로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 200과 JWT 토큰을 반환한다")
        void login_success() throws Exception {
            LoginRequestDto request = new LoginRequestDto("user1@test.com", "password123!");
            LoginResponseDto response = LoginResponseDto.of(2L, "mock.jwt.token");

            given(authService.login(any())).willReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                    .andExpect(jsonPath("$.data.grantType").value("Bearer"));
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 404를 반환한다")
        void login_userNotFound_returns400() throws Exception {
            LoginRequestDto request = new LoginRequestDto("none@test.com", "password123!");

            given(authService.login(any())).willThrow(new UserException(ErrorCode.USER_NOT_FOUND));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("비밀번호 불일치 시 401을 반환한다")
        void login_wrongPassword_returns400() throws Exception {
            LoginRequestDto request = new LoginRequestDto("user1@test.com", "wrongpassword!");

            given(authService.login(any())).willThrow(new UnauthorizedException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout - 로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 토큰으로 로그아웃 시 200을 반환한다")
        void logout_success() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer mock.jwt.token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 로그아웃 시 401을 반환한다")
        void logout_invalidToken_returns400() throws Exception {
            doThrow(new UnauthorizedException(ErrorCode.INVALID_TOKEN))
                    .when(authService).logout(any());

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer invalid.token"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}