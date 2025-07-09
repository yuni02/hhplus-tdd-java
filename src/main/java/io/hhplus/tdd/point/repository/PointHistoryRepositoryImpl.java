package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryTable pointHistoryTable;

    @Override
    public PointHistory save(long userId, long amount, TransactionType type, long updateMillis) {
        // 도메인의 정적 팩토리 메서드를 활용하여 유효성 검증
        // (단, ID는 테이블에서 자동 생성되므로 임시 ID 0 사용)
        if (TransactionType.CHARGE.equals(type)) {
            PointHistory.createCharge(0, userId, amount, updateMillis); // 유효성 검증 목적
        } else if (TransactionType.USE.equals(type)) {
            PointHistory.createUse(0, userId, amount, updateMillis); // 유효성 검증 목적
        }

        // 검증 통과 후 테이블에 저장
        return pointHistoryTable.insert(userId, amount, type, updateMillis);
    }

    @Override
    public PointHistory save(PointHistory pointHistory) {
        // 도메인 객체가 이미 검증되었다고 가정하고 테이블에 저장
        // (ID는 테이블에서 자동 생성)
        return pointHistoryTable.insert(
                pointHistory.userId(),
                pointHistory.amount(),
                pointHistory.type(),
                pointHistory.updateMillis());
    }

    @Override
    public List<PointHistory> findAllByUserId(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}