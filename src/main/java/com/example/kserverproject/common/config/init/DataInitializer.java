package com.example.kserverproject.common.config.init;

import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.repository.MenuRepository;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.enums.UserRole;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] 이미 데이터가 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        initUsers();
        initMenus();

        log.info("[DataInitializer] 더미 데이터 초기화 완료");
    }

    private void initUsers() {
        String encodedPassword = passwordEncoder.encode("password123!");

        // ADMIN 계정 1개
        User admin = User.builder()
                .email("admin@test.com")
                .password(encodedPassword)
                .nickname("관리자")
                .role(UserRole.ADMIN)
                .build();

        // CUSTOMER 계정 5개 (K6 동시성 테스트용으로 포인트 넉넉하게)
        User customer1 = User.builder()
                .email("user1@test.com")
                .password(encodedPassword)
                .nickname("테스터1")
                .role(UserRole.CUSTOMER)
                .build();

        User customer2 = User.builder()
                .email("user2@test.com")
                .password(encodedPassword)
                .nickname("테스터2")
                .role(UserRole.CUSTOMER)
                .build();

        User customer3 = User.builder()
                .email("user3@test.com")
                .password(encodedPassword)
                .nickname("테스터3")
                .role(UserRole.CUSTOMER)
                .build();

        User customer4 = User.builder()
                .email("user4@test.com")
                .password(encodedPassword)
                .nickname("테스터4")
                .role(UserRole.CUSTOMER)
                .build();

        User customer5 = User.builder()
                .email("user5@test.com")
                .password(encodedPassword)
                .nickname("테스터5")
                .role(UserRole.CUSTOMER)
                .build();

        userRepository.saveAll(List.of(admin, customer1, customer2, customer3, customer4, customer5));

        // 포인트 충전 (K6 부하 테스트에서 잔액 부족 없도록 충분히)
        customer1.chargePoint(1_000_000L);
        customer2.chargePoint(1_000_000L);
        customer3.chargePoint(1_000_000L);
        customer4.chargePoint(1_000_000L);
        customer5.chargePoint(1_000_000L);

        log.info("[DataInitializer] 유저 초기화 완료 - ADMIN 1명, CUSTOMER 5명");
    }

    private void initMenus() {
        List<Menu> menus = List.of(
                Menu.builder()
                        .menuName("아메리카노")
                        .price(3000L)
                        .imageUrl("https://example.com/images/americano.jpg")
                        .build(),
                Menu.builder()
                        .menuName("아이스 아메리카노")
                        .price(3500L)
                        .imageUrl("https://example.com/images/ice-americano.jpg")
                        .build(),
                Menu.builder()
                        .menuName("카페라떼")
                        .price(4000L)
                        .imageUrl("https://example.com/images/caffe-latte.jpg")
                        .build(),
                Menu.builder()
                        .menuName("카푸치노")
                        .price(4500L)
                        .imageUrl("https://example.com/images/cappuccino.jpg")
                        .build(),
                Menu.builder()
                        .menuName("바닐라라떼")
                        .price(4500L)
                        .imageUrl("https://example.com/images/vanilla-latte.jpg")
                        .build(),
                Menu.builder()
                        .menuName("카라멜 마키아토")
                        .price(5000L)
                        .imageUrl("https://example.com/images/caramel-macchiato.jpg")
                        .build()
        );

        menuRepository.saveAll(menus);

        log.info("[DataInitializer] 메뉴 초기화 완료 - {}개", menus.size());
    }
}
