package io.hhplus.tdd.point.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * 유저 포인트 조회
     * 
     * 1. 유저 포인트 조회
     * 2. 유저 포인트 반환
     * 
     * @param userId
     * @return
     */
    public UserPoint getUserPoint(long userId) {
        log.info("Getting user point for userId: {}", userId);
        return userPointTable.selectById(userId);
    }
    
    /**
     * 포인트 내역 조회
     * 
     * 1. 유저 포인트 내역 조회
     * 2. 유저 포인트 내역 반환
     * 
     * @param userId
     * @return
     */
    public List<PointHistory> getPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * 포인트 충전
     * 
     * 1. 유저 포인트 조회
     * 2. 유저 포인트가 0보다 작으면 예외 발생
     * 3. 유저 포인트 업데이트
     * 4. 포인트 내역 저장
     * 5. 유저 포인트 반환
     * @param userId
     * @param amount
     * @return
     */
    public UserPoint chargePoint(long userId, long amount) {
        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint.point() + amount < 0) {
            throw new IllegalArgumentException("잔고가 부족합니다");
        }
        userPointTable.insertOrUpdate(userId, userPoint.point() + amount);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return userPoint;
    }

    /* 
     * 포인트 사용
     * 
     * 1. 유저 포인트 조회
     * 2. 유저 포인트가 0보다 작으면 예외 발생
     * 3. 유저 포인트 업데이트
     * 4. 포인트 내역 저장
     * 5. 유저 포인트 반환
     * 
     * @param userId
     * @param amount
     * @return
     */
    public UserPoint usePoint(long userId, long amount) {   
        UserPoint userPoint = userPointTable.selectById(userId);
        if (userPoint.point() - amount < 0) {
            throw new IllegalArgumentException("잔고가 부족합니다");
        }
        userPointTable.insertOrUpdate(userId, userPoint.point() - amount);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        return userPoint;
    }
}
