package com.flyby.ramble.report.repository;

import com.flyby.ramble.report.model.QUserBan;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class UserBanRepositoryImpl implements UserBanRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;
    private final QUserBan userBan = QUserBan.userBan;

    @Override
    public boolean isUserCurrentlyBanned(Long userId) {
        BooleanExpression isCurrentlyBanned = userBan.banExpiresAt.gt(
                Expressions.dateTimeTemplate(LocalDateTime.class, "CURRENT_TIMESTAMP")
        );

        Integer result = jpaQueryFactory
                .selectOne()
                .from(userBan)
                .where(
                        userBan.bannedUser.id.eq(userId),
                        isCurrentlyBanned
                )
                .fetchFirst();

        return result != null;
    }
}
