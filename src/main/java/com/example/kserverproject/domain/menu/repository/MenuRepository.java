package com.example.kserverproject.domain.menu.repository;

import com.example.kserverproject.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long>, MenuQueryRepository {
}
