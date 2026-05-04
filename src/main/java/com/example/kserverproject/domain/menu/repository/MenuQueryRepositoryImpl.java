package com.example.kserverproject.domain.menu.repository;

import com.example.kserverproject.domain.menu.dto.request.MenuSearchRequestDto;
import com.example.kserverproject.domain.menu.entity.Menu;
import com.example.kserverproject.domain.menu.entity.QMenu;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MenuQueryRepositoryImpl implements MenuQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    QMenu qMenu = QMenu.menu;

    @Override
    public Page<Menu> searchMenus(MenuSearchRequestDto requestDto, Pageable pageable) {

        List<Menu> content = jpaQueryFactory
                .selectFrom(qMenu)
                .where(searchCondition(requestDto))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qMenu.count())
                .from(qMenu)
                .where(searchCondition(requestDto));

        return PageableExecutionUtils.getPage(content, pageable,
                () -> Optional.ofNullable(countQuery.fetchOne()).orElse(0L));
    }

    public Predicate searchCondition(MenuSearchRequestDto requestDto) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(nameContains(requestDto.keyword()));
        builder.and(priceGoe(requestDto.minPrice()));
        builder.and(priceLoe(requestDto.maxPrice()));

        return builder.getValue();
    }

    private BooleanExpression nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return qMenu.menuName.containsIgnoreCase(keyword);
    }

    private BooleanExpression priceGoe(Long minPrice) {
        if (minPrice == null || minPrice <= 0) {
            return null;
        }

        return qMenu.price.goe(minPrice);
    }

    private BooleanExpression priceLoe(Long maxPrice) {
        if (maxPrice == null || maxPrice <= 0) {
            return null;
        }
        return qMenu.price.loe(maxPrice);
    }
}
