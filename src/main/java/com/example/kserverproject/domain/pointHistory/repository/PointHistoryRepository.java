package com.example.kserverproject.domain.pointHistory.repository;

import com.example.kserverproject.domain.pointHistory.entity.PointHistory;
import com.example.kserverproject.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findAllByUser(User findUser, Pageable pageable);
}
