package com.example.kserverproject.domain.order.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.OrderException;
import com.example.kserverproject.common.exception.PointException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.order.dto.request.CreateOrderRequestDto;
import com.example.kserverproject.domain.order.dto.response.CreateOrderResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderCancelResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderDetailResponseDto;
import com.example.kserverproject.domain.order.dto.response.OrderListResponseDto;
import com.example.kserverproject.domain.order.enums.OrderStatus;
import com.example.kserverproject.domain.order.facade.OrderFacade;
import com.example.kserverproject.domain.order.service.OrderService;
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
        controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderFacade orderFacade;

    @Nested
    @DisplayName("POST /api/orders - 주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("정상 주문 생성 시 201을 반환한다")
        void createOrder_success() throws Exception {
            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 2))
            );

            CreateOrderResponseDto response = new CreateOrderResponseDto(
                    1L, 2L,
                    List.of(new CreateOrderResponseDto.OrderItemResponseDto(1L, "아메리카노", 2, 3000L)),
                    6000L, OrderStatus.CREATED, LocalDateTime.now()
            );

            given(orderFacade.createOrder(eq(2L), any())).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/orders")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(1))
                    .andExpect(jsonPath("$.data.totalAmount").value(6000))
                    .andExpect(jsonPath("$.data.orderStatus").value("CREATED"));
        }

        @Test
        @DisplayName("주문 항목이 비어있으면 400을 반환한다")
        void createOrder_emptyItems_returns400() throws Exception {
            CreateOrderRequestDto request = new CreateOrderRequestDto(List.of());

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/orders")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("포인트 잔액 부족 시 409를 반환한다")
        void createOrder_insufficientPoints_returns409() throws Exception {
            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(1L, 1))
            );

            given(orderFacade.createOrder(eq(99L), any()))
                    .willThrow(new PointException(ErrorCode.INSUFFICIENT_POINTS_BALANCE));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomerWithNoPoint());

            mockMvc.perform(post("/api/orders")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_POINTS_BALANCE"));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 주문 시 404를 반환한다")
        void createOrder_menuNotFound_returns404() throws Exception {
            CreateOrderRequestDto request = new CreateOrderRequestDto(
                    List.of(new CreateOrderRequestDto.OrderItemRequestDto(999L, 1))
            );

            given(orderFacade.createOrder(eq(2L), any()))
                    .willThrow(new MenuException(ErrorCode.MENU_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/orders")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("MENU_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{orderId} - 주문 상세 조회")
    class GetOrderDetail {

        @Test
        @DisplayName("본인의 주문 상세 조회 시 200을 반환한다")
        void getOrderDetail_success() throws Exception {
            OrderDetailResponseDto response = new OrderDetailResponseDto(
                    1L, 2L,
                    List.of(new OrderDetailResponseDto.OrderItemResponseDto(
                            1L, "아메리카노", 2, 3000L)),
                    6000L, OrderStatus.COMPLETED, LocalDateTime.now()
            );

            given(orderService.getOrderDetail(2L, 1L)).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/orders/1")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(1))
                    .andExpect(jsonPath("$.data.totalAmount").value(6000))
                    .andExpect(jsonPath("$.data.orderStatus").value("COMPLETED"));
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 404를 반환한다")
        void getOrderDetail_notFound_returns404() throws Exception {
            given(orderService.getOrderDetail(2L, 999L))
                    .willThrow(new OrderException(ErrorCode.ORDER_NOT_FOUND));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/orders/999")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        }

        @Test
        @DisplayName("타인의 주문 조회 시 403을 반환한다")
        void getOrderDetail_accessDenied_returns403() throws Exception {
            given(orderService.getOrderDetail(2L, 1L))
                    .willThrow(new OrderException(ErrorCode.ORDER_ACCESS_DENIED));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/orders/1")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ORDER_ACCESS_DENIED"));
        }
    }

    @Nested
    @DisplayName("GET /api/orders/me - 내 주문 목록 조회")
    class GetOrderList {

        @Test
        @DisplayName("내 주문 목록을 페이징하여 반환한다")
        void getOrderList_success() throws Exception {
            List<OrderListResponseDto> content = List.of(
                    new OrderListResponseDto(1L, 2L,
                            List.of(new OrderListResponseDto.OrderItemResponseDto(1L, "아메리카노", 2, 3000L)),
                            6000L, OrderStatus.COMPLETED, LocalDateTime.now())
            );
            PageResponseDto<OrderListResponseDto> response = new PageResponseDto<>(content, 1, 10, 1L);

            given(orderService.getOrderList(eq(2L), any())).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(get("/api/orders/me")
                            .with(user(userDetails))
                            .param("page", "1")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/orders/{orderId}/cancel - 주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("정상 취소 시 환불 금액과 잔액을 반환한다")
        void cancelOrder_success() throws Exception {
            OrderCancelResponseDto response = new OrderCancelResponseDto(
                    1L, OrderStatus.CANCELED, 6000L, 1_006_000L
            );

            given(orderService.cancelMyOrder(2L, 1L)).willReturn(response);

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/orders/1/cancel")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderStatus").value("CANCELED"))
                    .andExpect(jsonPath("$.data.refundedAmount").value(6000))
                    .andExpect(jsonPath("$.data.pointBalance").value(1_006_000));
        }

        @Test
        @DisplayName("CREATED 상태 주문 취소 시 400을 반환한다")
        void cancelOrder_invalidStatus_returns400() throws Exception {
            given(orderService.cancelMyOrder(2L, 1L))
                    .willThrow(new OrderException(ErrorCode.INVALID_ORDER_STATUS));

            CustomUserDetails userDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/orders/1/cancel")
                            .with(user(userDetails)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
        }
    }
}
