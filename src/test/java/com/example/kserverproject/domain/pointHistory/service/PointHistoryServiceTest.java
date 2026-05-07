package com.example.kserverproject.domain.pointHistory.service;

import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.pointHistory.dto.request.PointChargeRequestDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointBalanceResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointChargeResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointHistoryTransactionalResponseDto;
import com.example.kserverproject.domain.pointHistory.entity.PointHistory;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.pointHistory.repository.PointHistoryRepository;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointHistoryService 테스트")
class PointHistoryServiceTest {

    @InjectMocks
    private PointHistoryService pointHistoryService;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("포인트 잔액 조회")
    class GetMyPointBalance {

        @Test
        @DisplayName("유저의 포인트 잔액을 반환한다")
        void getMyPointBalance_success() {
            given(userRepository.findById(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));

            PointBalanceResponseDto response = pointHistoryService.getMyPointBalance(2L);

            assertThat(response.userId()).isEqualTo(2L);
            assertThat(response.pointBalance()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 UserException이 발생한다")
        void getMyPointBalance_userNotFound_throwsUserException() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> pointHistoryService.getMyPointBalance(999L))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("포인트 충전")
    class ChargePoint {

        @Test
        @DisplayName("정상 충전 시 잔액이 증가하고 이력이 저장된다")
        void chargePoint_success() {
            User customer = TestFixtures.createCustomer1(); // 1_000_000L
            PointChargeRequestDto request = new PointChargeRequestDto(5000L);

            given(userRepository.findByUserIdWithLock(2L)).willReturn(Optional.of(customer));
            given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(inv -> inv.getArgument(0));

            PointChargeResponseDto response = pointHistoryService.chargePoint(2L, request);

            assertThat(response.chargeAmount()).isEqualTo(5000L);
            assertThat(response.pointBalance()).isEqualTo(1_005_000L);
            verify(pointHistoryRepository).save(any(PointHistory.class));
        }

        @Test
        @DisplayName("존재하지 않는 유저 충전 시 UserException이 발생한다")
        void chargePoint_userNotFound_throwsUserException() {
            given(userRepository.findByUserIdWithLock(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> pointHistoryService.chargePoint(999L, new PointChargeRequestDto(5000L)))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("포인트 거래 내역 조회")
    class GetMyPointTransactionalRecord {

        @Test
        @DisplayName("포인트 거래 내역을 페이징하여 반환한다")
        void getMyPointTransactionalRecord_success() {
            User customer = TestFixtures.createCustomer1();
            PageRequest pageable = PageRequest.of(0, 10);

            given(userRepository.findById(2L)).willReturn(Optional.of(customer));
            given(pointHistoryRepository.findAllByUser(customer, pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            PageResponseDto<PointHistoryTransactionalResponseDto> result =
                    pointHistoryService.getMyPointTransactionalRecord(2L, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("포인트 이력 기록 (내부 호출)")
    class Record {

        @Test
        @DisplayName("PAYMENT 타입으로 포인트 이력을 저장한다")
        void record_payment() {
            User customer = TestFixtures.createCustomer1();
            given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(inv -> inv.getArgument(0));

            pointHistoryService.record(customer, 3000L, PointType.PAYMENT);

            verify(pointHistoryRepository).save(any(PointHistory.class));
        }

        @Test
        @DisplayName("REFUND 타입으로 포인트 이력을 저장한다")
        void record_refund() {
            User customer = TestFixtures.createCustomer1();
            given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(inv -> inv.getArgument(0));

            pointHistoryService.record(customer, 6000L, PointType.REFUND);

            verify(pointHistoryRepository).save(any(PointHistory.class));
        }
    }
}