package com.example.kserverproject.domain.user.entity;

import com.example.kserverproject.common.entity.BaseEntity;
import com.example.kserverproject.common.exception.ErrorCode;
import com.example.kserverproject.common.exception.PointException;
import com.example.kserverproject.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Long pointBalance;

    // 포인트 충전
    public void chargePoint(Long amount) {
        this.pointBalance += amount;
    }

    // 포인트 차감
    public void deductPoint(Long amount) {
        if (this.pointBalance < amount) {
            throw new PointException(ErrorCode.INSUFFICIENT_POINTS_BALANCE);
        }
        this.pointBalance -= amount;
    }

    // 포인트 환불
    public void refundPoint(Long amount) {
        this.pointBalance += amount;
    }

}
