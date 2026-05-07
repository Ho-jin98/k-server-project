package com.example.kserverproject.domain.menu.repository;

import com.example.kserverproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long>, MenuQueryRepository {

    // 메뉴 생성시 중복 방지
    boolean existsByMenuName(String menuName);

    // 메뉴 수정시 중복 방지
    boolean existsByMenuNameAndIdNot(String menuName, Long id);
}
