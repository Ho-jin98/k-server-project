package com.example.kserverproject.domain.pointHistory.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.auth.controller.AuthController;
import com.example.kserverproject.domain.pointHistory.dto.request.PointChargeRequestDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointBalanceResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointChargeResponseDto;
import com.example.kserverproject.domain.pointHistory.dto.response.PointHistoryTransactionalResponseDto;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.pointHistory.service.PointHistoryService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PointHistoryController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("PointHistoryController 테스트")
class PointHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointHistoryService pointHistoryService;

    @Nested
    @DisplayName("GET /api/points/me - 포인트 잔액 조회")
    class GetMyPointBalance {

        @Test
        @DisplayName("인증된 유저의 포인트 잔액을 반환한다")
        void getMyPointBalance_success() throws Exception {
            PointBalanceResponseDto response = new PointBalanceResponseDto(2L, 1_000_000L);
            given(pointHistoryService.getMyPointBalance(2L)).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/points/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.userId").value(2))
                    .andExpect(jsonPath("$.data.pointBalance").value(1_000_000));
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 400을 반환한다")
        void getMyPointBalance_userNotFound_returns400() throws Exception {
            given(pointHistoryService.getMyPointBalance(2L))
                    .willThrow(new UserException(ErrorCode.USER_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/points/me")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("CS_001"));
        }
    }

    @Nested
    @DisplayName("POST /api/points/me/charge - 포인트 충전")
    class ChargePoint {

        @Test
        @DisplayName("정상 충전 시 200과 충전 결과를 반환한다")
        void chargePoint_success() throws Exception {
            PointChargeRequestDto request = new PointChargeRequestDto(5000L);
            PointChargeResponseDto response = new PointChargeResponseDto(2L, 5000L, 1_005_000L);

            given(pointHistoryService.chargePoint(eq(2L), any())).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/points/me/charge")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.chargeAmount").value(5000))
                    .andExpect(jsonPath("$.data.pointBalance").value(1_005_000));
        }

        @Test
        @DisplayName("충전 금액이 0 이하이면 400을 반환한다")
        void chargePoint_invalidAmount_returns400() throws Exception {
            PointChargeRequestDto request = new PointChargeRequestDto(-1000L);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/points/me/charge")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("충전 금액이 null이면 400을 반환한다")
        void chargePoint_nullAmount_returns400() throws Exception {
            String requestBody = "{\"amount\": null}";

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/points/me/charge")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/points/me/histories - 포인트 거래 내역 조회")
    class GetMyPointHistories {

        @Test
        @DisplayName("포인트 거래 내역을 페이징하여 반환한다")
        void getMyPointHistories_success() throws Exception {
            List<PointHistoryTransactionalResponseDto> content = List.of(
                    new PointHistoryTransactionalResponseDto(1L, 5000L, PointType.CHARGE, 1_005_000L, LocalDateTime.now()),
                    new PointHistoryTransactionalResponseDto(2L, 3000L, PointType.PAYMENT, 1_002_000L, LocalDateTime.now())
            );
            PageResponseDto<PointHistoryTransactionalResponseDto> response =
                    new PageResponseDto<>(content, 1, 10, 2L);

            given(pointHistoryService.getMyPointTransactionalRecord(eq(2L), any())).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/points/me/histories")
                            .with(user(userDetails))
                            .param("page", "1")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[0].pointType").value("CHARGE"))
                    .andExpect(jsonPath("$.data.content[1].pointType").value("PAYMENT"));
        }

        @Test
        @DisplayName("거래 내역이 없으면 빈 페이지를 반환한다")
        void getMyPointHistories_empty() throws Exception {
            PageResponseDto<PointHistoryTransactionalResponseDto> response =
                    new PageResponseDto<>(List.of(), 1, 10, 0L);

            given(pointHistoryService.getMyPointTransactionalRecord(eq(2L), any())).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/points/me/histories")
                            .with(user(userDetails))
                            .param("page", "1")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }
}