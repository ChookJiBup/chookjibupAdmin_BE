package com.example.chookjibupadmin.festival.query.infrastructure.persistence;

import com.example.chookjibupadmin.festival.command.domain.QFestival;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import java.time.LocalDate;

/** 목록 표시, 필터, 정렬이 같은 진행 상태를 사용하도록 한다. */
public final class FestivalProgressExpression {
    private FestivalProgressExpression() { }

    public static StringExpression of(QFestival festival, LocalDate today) {
        StringExpression dateStatus = new CaseBuilder()
                .when(festival.period.startDate.isNull().or(festival.period.endDate.isNull()))
                .then(Expressions.stringTemplate("null"))
                .when(festival.period.startDate.gt(today)).then("UPCOMING")
                .when(festival.period.endDate.lt(today)).then("COMPLETED")
                .otherwise("ONGOING");
        return festival.progressStatusOverride.stringValue()
                .coalesce(festival.storedProgressStatus.upper())
                .coalesce(dateStatus);
    }
}
