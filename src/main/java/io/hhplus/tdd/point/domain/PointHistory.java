package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.point.domain.constant.TransactionType;

public record PointHistory(
        long id,
        long userId,
        long amount,
        TransactionType type,
        long updateMillis
) {
}
