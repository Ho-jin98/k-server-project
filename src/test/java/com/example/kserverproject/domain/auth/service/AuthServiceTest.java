package com.example.kserverproject.domain.auth.service;

import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.common.jwt.JwtUtil;
import com.example.kserverproject.domain.auth.dto.request.LoginRequestDto;
import com.example.kserverproject.domain.auth.dto.request.SignupRequestDto;
import com.example.kserverproject.domain.auth.dto.response.LoginResponseDto;
import com.example.kserverproject.domain.auth.dto.response.SignupResponseDto;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.example.kserverproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    @Mock
    private ValueOperations<Object, Object> valueOperations;

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("정상 회원가입 시 유저 정보를 반환한다")
        void signup_success() {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("user1@test.com")
                    .password("password123!")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            given(userRepository.existsByEmail("user1@test.com")).willReturn(false);
            given(passwordEncoder.encode("password123!")).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(TestFixtures.createCustomer1());

            SignupResponseDto response = authService.signup(request);

            assertThat(response.email()).isEqualTo("user1@test.com");
            assertThat(response.nickname()).isEqualTo("테스터1");
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("중복 이메일로 회원가입 시 UserException이 발생한다")
        void signup_duplicateEmail_throwsUserException() {
            SignupRequestDto request = SignupRequestDto.builder()
                    .email("user1@test.com")
                    .password("password123!")
                    .nickname("테스터1")
                    .role(UserRole.CUSTOMER)
                    .build();

            given(userRepository.existsByEmail("user1@test.com")).willReturn(true);

            assertThatThrownBy(() -> authService.signup(request))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 JWT 토큰을 반환한다")
        void login_success() {
            LoginRequestDto request = new LoginRequestDto("user1@test.com", "password123!");

            given(userRepository.findByEmail("user1@test.com"))
                    .willReturn(Optional.of(TestFixtures.createCustomer1()));
            given(passwordEncoder.matches("password123!", "encodedPassword")).willReturn(true);
            given(jwtUtil.generateToken(any(), any(), any())).willReturn("mock.jwt.token");

            LoginResponseDto response = authService.login(request);

            assertThat(response.token()).isEqualTo("mock.jwt.token");
            assertThat(response.grantType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 UserException이 발생한다")
        void login_userNotFound_throwsUserException() {
            LoginRequestDto request = new LoginRequestDto("none@test.com", "password123!");

            given(userRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UserException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 UnauthorizedException이 발생한다")
        void login_wrongPassword_throwsUnauthorizedException() {
            LoginRequestDto request = new LoginRequestDto("user1@test.com", "wrongpassword!");

            given(userRepository.findByEmail("user1@test.com"))
                    .willReturn(Optional.of(TestFixtures.createCustomer1()));
            given(passwordEncoder.matches("wrongpassword!", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 토큰으로 로그아웃 시 블랙리스트에 저장된다")
        void logout_success() {
            given(jwtUtil.validateToken("valid.token")).willReturn(true);
            given(jwtUtil.getRemainingTime("valid.token")).willReturn(3_600_000L);
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            authService.logout("valid.token");

            verify(valueOperations).set(any(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 로그아웃 시 UnauthorizedException이 발생한다")
        void logout_invalidToken_throwsUnauthorizedException() {
            given(jwtUtil.validateToken("invalid.token")).willReturn(false);

            assertThatThrownBy(() -> authService.logout("invalid.token"))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}