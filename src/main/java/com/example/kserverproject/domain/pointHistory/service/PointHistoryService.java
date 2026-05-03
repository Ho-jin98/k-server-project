package com.example.kserverproject.domain.pointHistory.service;

import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.pointHistory.dto.request.PointChargeRequestDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointBalanceResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointChargeResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointHistoryTransactionalResponseDto;
import com.example.kserverproject.domain.pointHistory.entity.PointHistory;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.pointHistory.repository.PointHistoryRepository;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    // 내 포인트 잔액 조회
    public PointBalanceResponseDto getMyPointBalance(Long userId) {
        User findUser = userRepository.findById(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        return PointBalanceResponseDto.from(findUser);
    }

    // 포인트 충전
    @Transactional
    public PointChargeResponseDto chargePoint(Long userId, PointChargeRequestDto requestDto) {
        User findUser = userRepository.findByUserIdWithLock(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        findUser.chargePoint(requestDto.amount());

        PointHistory pointHistory = PointHistory.builder()
                .user(findUser)
                .amount(requestDto.amount())
                .pointType(PointType.CHARGE)
                .balanceAfter(findUser.getPointBalance())
                .build();

        pointHistoryRepository.save(pointHistory);

        return PointChargeResponseDto.of(findUser, pointHistory.getAmount());
    }

    // 내 포인트 거래목록 조회
    public PageResponseDto<PointHistoryTransactionalResponseDto> getMyPointTransactionalRecord(Long userId, Pageable pageable) {

        User findUser = userRepository.findById(userId)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));

        Page<PointHistoryTransactionalResponseDto> result = pointHistoryRepository.findAllByUser(findUser, pageable)
                .map(PointHistoryTransactionalResponseDto::from);

        return PageResponseDto.of(result);
    }

    // Order에서 호출할 결제/환불 기록 저장 메서드
    public void record(User user, Long amount, PointType pointType) {
        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .amount(amount)
                .pointType(pointType)
                .balanceAfter(user.getPointBalance())
                .build();

        pointHistoryRepository.save(pointHistory);
    }
}
