package com.example.kserverproject.domain.pointHistory.entity;

import com.example.kserverproject.common.entity.BaseEntity;
import com.example.kserverproject.domain.pointHistory.enums.PointType;
import com.example.kserverproject.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "amounts", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "point_type", nullable = false)
    private PointType pointType;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter; // 포인트 거래 후 잔액

    @Builder
    public PointHistory(User user, Long amount, PointType pointType, Long balanceAfter) {
        this.user = user;
        this.amount = amount;
        this.pointType = pointType;
        this.balanceAfter = balanceAfter;
    }
}
