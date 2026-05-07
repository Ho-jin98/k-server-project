package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.fixture.TestFixtures;
import com.example.kserverproject.domain.menu.dto.request.CreateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.request.UpdateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.response.CreateMenuResponseDto;
import com.example.kserverproject.domain.menu.dto.response.UpdateMenuResponseDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminMenuService 테스트")
class AdminMenuServiceTest {

    @InjectMocks
    private AdminMenuService adminMenuService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MenuRepository menuRepository;

    @Nested
    @DisplayName("메뉴 생성")
    class CreateMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴를 생성한다")
        void createMenu_success() {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));
            given(menuRepository.save(any(Menu.class))).willAnswer(inv -> inv.getArgument(0));

            CreateMenuResponseDto response = adminMenuService.createMenu(1L, request);

            assertThat(response.menuName()).isEqualTo("바닐라라떼");
            assertThat(response.price()).isEqualTo(4500L);
        }

        @Test
        @DisplayName("CUSTOMER 권한으로 메뉴 생성 시 UnauthorizedException이 발생한다")
        void createMenu_notAdmin_throwsUnauthorizedException() {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            given(userRepository.findById(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));

            assertThatThrownBy(() -> adminMenuService.createMenu(2L, request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저로 생성 시 UserException이 발생한다")
        void createMenu_userNotFound_throwsUserException() {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "바닐라라떼", 4500L, "https://example.com/images/vanilla-latte.jpg"
            );

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminMenuService.createMenu(999L, request))
                    .isInstanceOf(UserException.class);
        }
    }

    @Nested
    @DisplayName("메뉴 수정")
    class UpdateMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴를 수정한다")
        void updateMenu_success() {
            UpdateMenuRequestDto request = new UpdateMenuRequestDto(
                    "수정된아메리카노", 3500L, "https://example.com/images/new.jpg"
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));
            given(menuRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAmericano()));

            UpdateMenuResponseDto response = adminMenuService.updateMenu(1L, request, 1L);

            assertThat(response.menuName()).isEqualTo("수정된아메리카노");
            assertThat(response.price()).isEqualTo(3500L);
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 수정 시 MenuException이 발생한다")
        void updateMenu_menuNotFound_throwsMenuException() {
            UpdateMenuRequestDto request = new UpdateMenuRequestDto(
                    "수정된아메리카노", 3500L, "https://example.com/images/new.jpg"
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));
            given(menuRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminMenuService.updateMenu(1L, request, 999L))
                    .isInstanceOf(MenuException.class);
        }

        @Test
        @DisplayName("이미 존재하는 메뉴 이름으로 생성 시 MenuException이 발생한다")
        void createMenu_duplicateName_throwsMenuException() {
            CreateMenuRequestDto request = new CreateMenuRequestDto(
                    "아메리카노", 3000L, "https://example.com/images/americano.jpg"
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));
            given(menuRepository.existsByMenuName("아메리카노")).willReturn(true);

            assertThatThrownBy(() -> adminMenuService.createMenu(1L, request))
                    .isInstanceOf(MenuException.class);
        }
    }

    @Nested
    @DisplayName("메뉴 삭제")
    class DeleteMenu {

        @Test
        @DisplayName("ADMIN 권한으로 메뉴를 삭제한다")
        void deleteMenu_success() {
            given(userRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAdmin()));
            given(menuRepository.findById(1L)).willReturn(Optional.of(TestFixtures.createAmericano()));

            adminMenuService.deleteMenu(1L, 1L);

            verify(menuRepository).delete(any(Menu.class));
        }

        @Test
        @DisplayName("CUSTOMER 권한으로 메뉴 삭제 시 UnauthorizedException이 발생한다")
        void deleteMenu_notAdmin_throwsUnauthorizedException() {
            given(userRepository.findById(2L)).willReturn(Optional.of(TestFixtures.createCustomer1()));

            assertThatThrownBy(() -> adminMenuService.deleteMenu(2L, 1L))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}