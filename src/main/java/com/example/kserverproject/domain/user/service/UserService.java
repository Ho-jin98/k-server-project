package com.example.kserverproject.domain.user.service;

import com.example.kserverproject.common.exception.BusinessException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.user.dto.response.UserInformationResponseDto;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserInformationResponseDto getUserInformation(Long userId) {

        // 최신 데이터 보장 + 토큰 유효 유무 + 탈퇴한 유저 확인을 위한 DB 조회
        User findUser = userRepository.findById(userId)
                .orElseThrow( () -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserInformationResponseDto.from(findUser);
    }
}
