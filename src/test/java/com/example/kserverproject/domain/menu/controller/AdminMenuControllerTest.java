package com.example.kserverproject.domain.menu.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.common.jwt.CustomUserDetails;
import com.example.kserverproject.domain.auth.controller.AuthController;
import com.example.kserverproject.domain.menu.dto.request.CreateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.request.UpdateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.response.CreateMenuResponseDto;
import com.example.kserverproject.domain.menu.dto.response.UpdateMenuResponseDto;
import com.example.kserverproject.domain.menu.service.AdminMenuService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminMenuController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("AdminMenuController 테스트")
class AdminMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminMenuService adminMenuService;

    @Nested
    @DisplayName("POST /api/admin/menus - 메뉴 등록")
    class CreateMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴 등록 시 201을 반환한다")
        void createMenu_success() throws Exception {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );
            CreateMenuResponseDto response = new CreateMenuResponseDto(
                    4L, "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            given(adminMenuService.createMenu(any(), any())).willReturn(response);

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(post("/api/admin/menus")
                            .with(user(adminDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.menuName").value("바닐라라떼"))
                    .andExpect(jsonPath("$.data.price").value(4500));
        }

        @Test
        @DisplayName("메뉴 이름이 빈 값이면 400을 반환한다")
        void createMenu_blankName_returns400() throws Exception {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(post("/api/admin/menus")
                            .with(user(adminDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("가격이 0 이하이면 400을 반환한다")
        void createMenu_invalidPrice_returns400() throws Exception {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", -1000L, "https://example.com/images/vanilla-latte.jpg"
            );

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(post("/api/admin/menus")
                            .with(user(adminDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CUSTOMER 권한으로 메뉴 등록 시 400을 반환한다")
        void createMenu_notAdmin_returns400() throws Exception {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            given(adminMenuService.createMenu(any(), any()))
                    .willThrow(new UnauthorizedException(ErrorCode.UNAUTHORIZED_ADMIN_ACCESS));

            CustomUserDetails customerDetails = new CustomUserDetails(TestFixtures.createCustomer1());

            mockMvc.perform(post("/api/admin/menus")
                            .with(user(customerDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("ADMIN_001"));
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/menus/{menuId} - 메뉴 수정")
    class UpdateMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴 수정 시 200을 반환한다")
        void updateMenu_success() throws Exception {
            UpdateMenuRequestDto request = new UpdateMenuRequestDto(
                    "수정된라떼", 5000L, "https://example.com/images/new.jpg"
            );
            UpdateMenuResponseDto response = new UpdateMenuResponseDto(
                    1L, "수정된라떼", 5000L, "https://example.com/images/new.jpg"
            );

            given(adminMenuService.updateMenu(any(), any(), eq(1L))).willReturn(response);

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(put("/api/admin/menus/1")
                            .with(user(adminDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.menuName").value("수정된라떼"))
                    .andExpect(jsonPath("$.data.price").value(5000));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 수정 시 400을 반환한다")
        void updateMenu_notFound_returns400() throws Exception {
            UpdateMenuRequestDto request = new UpdateMenuRequestDto(
                    "수정된라떼", 5000L, "https://example.com/images/new.jpg"
            );

            given(adminMenuService.updateMenu(any(), any(), eq(999L)))
                    .willThrow(new MenuException(ErrorCode.MENU_NOT_FOUND));

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(put("/api/admin/menus/999")
                            .with(user(adminDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MENU_002"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/menus/{menuId} - 메뉴 삭제")
    class DeleteMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴 삭제 시 204를 반환한다")
        void deleteMenu_success() throws Exception {
            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(delete("/api/admin/menus/1")
                            .with(user(adminDetails)))
                    .andDo(print())
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 삭제 시 400을 반환한다")
        void deleteMenu_notFound_returns400() throws Exception {
            doThrow(new MenuException(ErrorCode.MENU_NOT_FOUND))
                    .when(adminMenuService).deleteMenu(any(), eq(999L));

            CustomUserDetails adminDetails = new CustomUserDetails(TestFixtures.createAdmin());

            mockMvc.perform(delete("/api/admin/menus/999")
                            .with(user(adminDetails)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MENU_002"));
        }
    }
}