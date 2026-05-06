package com.example.kserverproject.domain.auth.service;

import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.jwt.JwtUtil;
import com.example.kserverproject.domain.auth.dto.request.LoginRequestDto;
import com.example.kserverproject.domain.auth.dto.request.SignupRequestDto;
import com.example.kserverproject.domain.auth.dto.response.LoginResponseDto;
import com.example.kserverproject.domain.auth.dto.response.SignupResponseDto;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<Object, Object> redisTemplate;

    public SignupResponseDto signup(SignupRequestDto requestDto) {

        boolean existsUser = userRepository.existsByEmail(requestDto.email());

        if (existsUser) {
            throw new UserException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(requestDto.email())
                .password(passwordEncoder.encode(requestDto.password()))
                .nickname(requestDto.nickname())
                .role(requestDto.role())
                .build();

        User savedUser = userRepository.save(user);

        return SignupResponseDto.from(savedUser);

    }

    public LoginResponseDto login(LoginRequestDto requestDto) {

        // 유저 검증
        User user = userRepository.findByEmail(requestDto.email())
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 비밀 번호 검증
        if (!passwordEncoder.matches(requestDto.password(), user.getPassword())) {
            throw new UnauthorizedException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        // 토큰 생성
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return LoginResponseDto.of(user.getId(), accessToken);
    }

    public void logout(String token) {
        // 토큰 유효성 검증
        if (token == null || !jwtUtil.validateToken(token)) {
            throw new UnauthorizedException(ErrorCode.INVALID_TOKEN);
        }

        long remainingTime = jwtUtil.getRemainingTime(token);

        // 남은 만료시간이 있을 때만 블랙리스트에 저장
        if (remainingTime > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + token, "logout", remainingTime, TimeUnit.MILLISECONDS);
        }
    }
}
