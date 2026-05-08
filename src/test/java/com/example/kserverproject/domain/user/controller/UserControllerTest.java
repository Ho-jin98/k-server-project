package com.example.kserverproject.domain.user.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.exception.BusinessException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.user.dto.response.UserInformationResponseDto;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.example.kserverproject.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("GET /api/users/me - 내 정보 조회")
    class GetUserInformation {

        @Test
        @DisplayName("인증된 유저가 내 정보를 조회하면 200을 반환한다")
        void getUserInformation_success() throws Exception {
            UserInformationResponseDto response = new UserInformationResponseDto(
                    2L, "user1@test.com", "테스터1", UserRole.CUSTOMER
            );

            given(userService.getUserInformation(2L)).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/users/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value("user1@test.com"))
                    .andExpect(jsonPath("$.data.nickname").value("테스터1"))
                    .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 404를 반환한다")
        void getUserInformation_notFound_returns400() throws Exception {
            given(userService.getUserInformation(2L))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/users/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}