package com.example.kserverproject.domain.menu.service;

import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.UnauthorizedException;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.domain.menu.dto.request.CreateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.request.UpdateMenuRequestDto;
import com.example.kserverproject.domain.menu.dto.response.CreateMenuResponseDto;
import com.example.kserverproject.domain.menu.dto.response.UpdateMenuResponseDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMenuService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

    // 메뉴 생성
    @Transactional
    public CreateMenuResponseDto createMenu(Long adminId, CreateMenuRequestDto requestDto) {

        User findUser = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (findUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ADMIN_ACCESS);
        }

        Menu createMenu = Menu.builder()
                .menuName(requestDto.menuName())
                .price(requestDto.price())
                .build();

        menuRepository.save(createMenu);

        return CreateMenuResponseDto.from(createMenu);
    }

    // 메뉴 수정
    @Transactional
    public UpdateMenuResponseDto updateMenu(Long adminId, UpdateMenuRequestDto requestDto, Long menuId) {

        User findUser = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (findUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ADMIN_ACCESS);
        }

        Menu findMenu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        findMenu.updateMenu(requestDto.menuName(), requestDto.price());

        return UpdateMenuResponseDto.from(findMenu);
    }

    // 메뉴 삭제
    @Transactional
    public void deleteMenu(Long adminId, Long menuId) {

        User findUser = userRepository.findById(adminId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (findUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException(ErrorCode.UNAUTHORIZED_ADMIN_ACCESS);
        }

        Menu findMenu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuException(ErrorCode.MENU_NOT_FOUND));

        menuRepository.delete(findMenu);
    }
}
