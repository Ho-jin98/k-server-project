package com.example.kserverproject.domain.user.service;

import com.example.kserverproject.common.exception.BusinessException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.user.dto.response.UserInformationResponseDto;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.example.kserverproject.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("내 정보 조회")
    class GetUserInformation {

        @Test
        @DisplayName("존재하는 유저 조회 시 유저 정보를 반환한다")
        void getUserInformation_customer_success() {
            given(userRepository.findById(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));

            UserInformationResponseDto response = userService.getUserInformation(2L);

            assertThat(response.userId()).isEqualTo(2L);
            assertThat(response.email()).isEqualTo("user1@test.com");
            assertThat(response.nickname()).isEqualTo("테스터1");
            assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("ADMIN 유저 조회 시 ADMIN 역할이 반환된다")
        void getUserInformation_admin_success() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));

            UserInformationResponseDto response = userService.getUserInformation(1L);

            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("admin@test.com");
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 BusinessException이 발생한다")
        void getUserInformation_notFound_throwsBusinessException() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserInformation(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}