package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.constant.TransactionType;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory save(long userId, long amount, TransactionType type, long updateMillis);

    /**
     * 도메인 객체를 직접 저장 (권장)
     */
    PointHistory save(PointHistory pointHistory);

    List<PointHistory> findAllByUserId(long userId);
}