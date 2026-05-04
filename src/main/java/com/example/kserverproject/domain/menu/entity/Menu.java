package com.example.kserverproject.domain.menu.entity;

import com.example.kserverproject.common.entity.BaseEntity;
import com.example.kserverproject.common.exception.MenuException;
import com.example.kserverproject.common.exception.enums.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE menus SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String menuName;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Builder
    public Menu(String menuName, Long price, String imageUrl) {
        this.menuName = menuName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isDeleted = false;
    }

    public void updateMenu(String menuName, Long price) {
        this.menuName = menuName;
        if (price == null || price <= 0) {
            throw new MenuException(ErrorCode.INVALID_MENU_PRICE);
        }
        this.price = price;
    }
}
