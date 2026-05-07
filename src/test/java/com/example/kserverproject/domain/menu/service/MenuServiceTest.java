package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.common.dto.response.PageResponseDto;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.menu.dto.request.MenuSearchRequestDto;
import com.example.kserverproject.domain.menu.dto.response.MenuDetailResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuListResponseDto;
import com.example.kserverproject.domain.menu.dto.response.MenuSearchResponseDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("MenuService 테스트")
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock
    private MenuRepository menuRepository;

    @Nested
    @DisplayName("메뉴 목록 조회")
    class GetMenus {

        @Test
        @DisplayName("전체 메뉴 목록을 반환한다")
        void getMenus_success() {
            given(menuRepository.findAll()).willReturn(List.of(
                    TestFixtures.createAmericano(),
                    TestFixtures.createLatte(),
                    TestFixtures.createCappuccino()
            ));

            List<MenuListResponseDto> result = menuService.getMenus();

            assertThat(result).hasSize(3);
            assertThat(result.get(0).menuName()).isEqualTo("아메리카노");
            assertThat(result.get(0).price()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("메뉴가 없으면 빈 리스트를 반환한다")
        void getMenus_empty() {
            given(menuRepository.findAll()).willReturn(List.of());

            List<MenuListResponseDto> result = menuService.getMenus();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("메뉴 상세 조회")
    class GetMenu {

        @Test
        @DisplayName("존재하는 메뉴 ID로 상세 정보를 조회한다")
        void getMenu_success() {
            given(menuRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAmericano()));

            MenuDetailResponseDto result = menuService.getMenu(1L);

            assertThat(result.menuName()).isEqualTo("아메리카노");
            assertThat(result.price()).isEqualTo(3000L);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 ID 조회 시 MenuException이 발생한다")
        void getMenu_notFound_throwsMenuException() {
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> menuService.getMenu(999L))
                    .isInstanceOf(MenuException.class);
        }
    }

    @Nested
    @DisplayName("메뉴 검색")
    class SearchMenus {

        @Test
        @DisplayName("키워드로 메뉴 검색 시 결과를 반환한다")
        void searchMenus_byKeyword() {
            MenuSearchRequestDto requestDto = new MenuSearchRequestDto("아메리카노", null, null, 1, 10);
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Menu> page = new PageImpl<>(List.of(TestFixtures.createAmericano()), pageable, 1);

            given(menuRepository.searchMenus(requestDto, pageable)).willReturn(page);

            PageResponseDto<MenuSearchResponseDto> result = menuService.searchMenus(requestDto, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).menuName()).isEqualTo("아메리카노");
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("가격 범위로 메뉴 검색 시 결과를 반환한다")
        void searchMenus_byPriceRange() {
            MenuSearchRequestDto requestDto = new MenuSearchRequestDto(null, 3000L, 4000L, 1, 10);
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Menu> page = new PageImpl<>(
                    List.of(TestFixtures.createAmericano(), TestFixtures.createLatte()), pageable, 2
            );

            given(menuRepository.searchMenus(requestDto, pageable)).willReturn(page);

            PageResponseDto<MenuSearchResponseDto> result = menuService.searchMenus(requestDto, pageable);

            assertThat(result.content()).hasSize(2);
            assertThat(result.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 페이지를 반환한다")
        void searchMenus_noResult() {
            MenuSearchRequestDto requestDto = new MenuSearchRequestDto("없는메뉴", null, null, 1, 10);
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Menu> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(menuRepository.searchMenus(requestDto, pageable)).willReturn(emptyPage);

            PageResponseDto<MenuSearchResponseDto> result = menuService.searchMenus(requestDto, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isEqualTo(0L);
        }
    }
}