package com.example.kserverproject.domain.menu.controller;

import com.example.kserverproject.common.config.JpaAuditingConfig;
import com.example.kserverproject.common.config.TestSecurityConfig;
import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.dto.response.MenuDetailResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuListResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuSearchResponseDto;
import com.example.kserverproject.domain.menu.dto.response.PopularMenuResponseDto;
import com.example.kserverproject.domain.menu.service.MenuPopularService;
import com.example.kserverproject.domain.menu.service.MenuService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MenuController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JpaAuditingConfig.class
        )
)
@Import(TestSecurityConfig.class)
@DisplayName("MenuController 테스트")
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private MenuPopularService menuPopularService;

    @Nested
    @DisplayName("GET /api/menus - 메뉴 목록 조회")
    class GetMenus {

        @Test
        @DisplayName("메뉴 목록 조회 시 200과 전체 메뉴를 반환한다")
        void getMenus_success() throws Exception {
            List<MenuListResponseDto> response = List.of(
                    new MenuListResponseDto(1L, "아메리카노", 3000L, "https://example.com/images/americano.jpg"),
                    new MenuListResponseDto(2L, "카페라떼", 4000L, "https://example.com/images/caffe-latte.jpg"),
                    new MenuListResponseDto(3L, "카푸치노", 4500L, "https://example.com/images/cappuccino.jpg")
            );

            given(menuService.getMenus()).willReturn(response);

            mockMvc.perform(get("/api/menus"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].menuName").value("아메리카노"))
                    .andExpect(jsonPath("$.data[0].price").value(3000));
        }

        @Test
        @DisplayName("메뉴가 없으면 빈 리스트를 반환한다")
        void getMenus_empty() throws Exception {
            given(menuService.getMenus()).willReturn(List.of());

            mockMvc.perform(get("/api/menus"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/menus/{menuId} - 메뉴 상세 조회")
    class GetMenu {

        @Test
        @DisplayName("존재하는 메뉴 조회 시 200과 메뉴 정보를 반환한다")
        void getMenu_success() throws Exception {
            MenuDetailResponseDto response = new MenuDetailResponseDto(
                    1L, "아메리카노", 3000L, "https://example.com/images/americano.jpg"
            );

            given(menuService.getMenu(1L)).willReturn(response);

            mockMvc.perform(get("/api/menus/1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.menuId").value(1))
                    .andExpect(jsonPath("$.data.menuName").value("아메리카노"))
                    .andExpect(jsonPath("$.data.price").value(3000));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 조회 시 404를 반환한다")
        void getMenu_notFound_returns404() throws Exception {
            given(menuService.getMenu(999L))
                    .willThrow(new MenuException(ErrorCode.MENU_NOT_FOUND));

            mockMvc.perform(get("/api/menus/999"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("MENU_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/menus/popular - 인기 메뉴 조회")
    class GetPopularMenus {

        @Test
        @DisplayName("인기 메뉴 TOP3를 반환한다")
        void getPopularMenus_success() throws Exception {
            List<PopularMenuResponseDto> response = List.of(
                    new PopularMenuResponseDto(1, 1L, "아메리카노", 3000L, "https://example.com/images/americano.jpg", 120),
                    new PopularMenuResponseDto(2, 3L, "카푸치노", 4500L, "https://example.com/images/cappuccino.jpg", 98),
                    new PopularMenuResponseDto(3, 2L, "카페라떼", 4000L, "https://example.com/images/caffe-latte.jpg", 87)
            );

            given(menuPopularService.getPopularMenus(3)).willReturn(response);

            mockMvc.perform(get("/api/menus/popular"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(3))
                    .andExpect(jsonPath("$.data[0].rank").value(1))
                    .andExpect(jsonPath("$.data[0].menuName").value("아메리카노"))
                    .andExpect(jsonPath("$.data[0].orderCount").value(120));
        }

        @Test
        @DisplayName("인기 메뉴 데이터가 없으면 빈 리스트를 반환한다")
        void getPopularMenus_empty() throws Exception {
            given(menuPopularService.getPopularMenus(3)).willReturn(List.of());

            mockMvc.perform(get("/api/menus/popular"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/menus/search - 메뉴 검색")
    class SearchMenus {

        @Test
        @DisplayName("키워드로 검색 시 200과 결과를 반환한다")
        void searchMenus_success() throws Exception {
            PageResponseDto<MenuSearchResponseDto> response = new PageResponseDto<>(
                    List.of(new MenuSearchResponseDto(1L, "아메리카노", 3000L, "https://example.com/images/americano.jpg")),
                    1, 10, 1L
            );

            given(menuService.searchMenus(any(), any(), any())).willReturn(response);

            mockMvc.perform(get("/api/menus/search")
                            .param("keyword", "아메리카노")
                            .param("page", "1")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
        void searchMenus_noResult() throws Exception {
            PageResponseDto<MenuSearchResponseDto> response = new PageResponseDto<>(
                    List.of(), 1, 10, 0L
            );

            given(menuService.searchMenus(any(), any(), any())).willReturn(response);

            mockMvc.perform(get("/api/menus/search")
                            .param("keyword", "없는메뉴")
                            .param("page", "1")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }
}
