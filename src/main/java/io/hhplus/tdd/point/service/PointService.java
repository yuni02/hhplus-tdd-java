package io.hhplus.tdd.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

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
        return userPointRepository.findById(userId);
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
        return pointHistoryRepository.findAllByUserId(userId);
    }

    /**
     * 포인트 충전
     * 
     * 1. 유저 포인트 조회
     * 2. 도메인 객체를 통한 충전 로직 실행 (유효성 검증 포함)
     * 3. 업데이트된 포인트 저장
     * 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
     * 5. 업데이트된 유저 포인트 반환
     * 
     * @param userId
     * @param amount
     * @return
     */
    public UserPoint chargePoint(long userId, long amount) {
        log.info("Charging {} points for userId: {}", amount, userId);

        // 1. 현재 유저 포인트 조회
        UserPoint currentUserPoint = userPointRepository.findById(userId);

        // 2. 도메인 객체를 통한 충전 (유효성 검증 포함)
        UserPoint updatedUserPoint = currentUserPoint.charge(amount);

        // 3. 업데이트된 포인트 저장
        userPointRepository.save(userId, updatedUserPoint.point());

        // 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
        PointHistory chargeHistory = PointHistory.createCharge(0, userId, amount, updatedUserPoint.updateMillis());
        pointHistoryRepository.save(chargeHistory);

        return updatedUserPoint;
    }

    /*
     * 포인트 사용
     * 
     * 1. 유저 포인트 조회
     * 2. 도메인 객체를 통한 사용 로직 실행 (유효성 검증 및 잔고 확인 포함)
     * 3. 업데이트된 포인트 저장
     * 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
     * 5. 업데이트된 유저 포인트 반환
     * 
     * @param userId
     * 
     * @param amount
     * 
     * @return
     */
    public UserPoint usePoint(long userId, long amount) {
        log.info("Using {} points for userId: {}", amount, userId);

        // 1. 현재 유저 포인트 조회
        UserPoint currentUserPoint = userPointRepository.findById(userId);

        // 2. 도메인 객체를 통한 사용 (유효성 검증 및 잔고 확인 포함)
        UserPoint updatedUserPoint = currentUserPoint.use(amount);

        // 3. 업데이트된 포인트 저장
        userPointRepository.save(userId, updatedUserPoint.point());

        // 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
        PointHistory useHistory = PointHistory.createUse(0, userId, amount, updatedUserPoint.updateMillis());
        pointHistoryRepository.save(useHistory);

        return updatedUserPoint;
    }
}
