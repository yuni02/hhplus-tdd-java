package io.hhplus.tdd.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Isolation; // 제거
// import org.springframework.transaction.annotation.Transactional; // 제거

import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
@Service
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // ReentrantLock 사용 (더 세밀한 제어 가능)
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    private ReentrantLock getUserLock(long userId) {
        return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
    }

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
     * 포인트 충전 - 동시성 제어 적용
     * 
     * 동시성 제어 방식:
     * 1. 사용자별 ReentrantLock을 통한 Critical Section 보호
     * 2. 유저별 독립적인 Lock으로 전체 시스템 성능 최적화
     * 3. UserPointTable의 HashMap Thread-Safety 문제 해결을 위한 강화된 동기화
     * 
     * 주의: UserPointTable이 Thread-Safe하지 않은 HashMap을 사용하므로
     * 조회-계산-저장 전체 과정을 하나의 원자적 연산으로 처리
     */
    public UserPoint chargePoint(long userId, long amount) {
        log.info("Charging {} points for userId: {}", amount, userId);

        ReentrantLock userLock = getUserLock(userId);
        userLock.lock();
        try {
            // 1. 현재 유저 포인트 조회 (Critical Section 내부에서)
            UserPoint currentUserPoint = userPointRepository.findById(userId);

            // 2. 도메인 객체를 통한 충전 (유효성 검증 포함)
            UserPoint updatedUserPoint = currentUserPoint.charge(amount);

            // 3. 업데이트된 포인트 저장 (Critical Section 내부에서 즉시 처리)
            // HashMap의 Thread-Safety 문제를 방지하기 위해 저장까지 한 번에 처리
            UserPoint savedUserPoint = userPointRepository.save(userId, updatedUserPoint.point());

            // 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
            PointHistory chargeHistory = PointHistory.createCharge(0, userId, amount, savedUserPoint.updateMillis());
            pointHistoryRepository.save(chargeHistory);

            log.info("Successfully charged {} points for userId: {}. New balance: {}",
                    amount, userId, savedUserPoint.point());

            // 저장된 실제 값을 반환하여 데이터 일관성 보장
            return savedUserPoint;
        } catch (Exception e) {
            log.error("Failed to charge {} points for userId: {}. Error: {}", amount, userId, e.getMessage());
            throw e;
        } finally {
            userLock.unlock();
        }
    }

    /**
     * 포인트 사용 - 동시성 제어 적용
     * 
     * 동시성 제어 방식:
     * 1. 사용자별 ReentrantLock을 통한 Critical Section 보호
     * 2. 잔고 부족 상황에서의 Race Condition 방지
     * 3. 동시 사용 요청 시 데이터 무결성 보장
     * 4. UserPointTable의 HashMap Thread-Safety 문제 해결을 위한 강화된 동기화
     * 
     * 주의: UserPointTable이 Thread-Safe하지 않은 HashMap을 사용하므로
     * 조회-검증-저장 전체 과정을 하나의 원자적 연산으로 처리
     */
    public UserPoint usePoint(long userId, long amount) {
        log.info("Using {} points for userId: {}", amount, userId);

        ReentrantLock userLock = getUserLock(userId);
        userLock.lock();
        try {
            // 1. 현재 유저 포인트 조회 (Critical Section 내부에서)
            UserPoint currentUserPoint = userPointRepository.findById(userId);

            // 2. 도메인 객체를 통한 사용 (유효성 검증 및 잔고 확인 포함)
            UserPoint updatedUserPoint = currentUserPoint.use(amount);

            // 3. 업데이트된 포인트 저장 (Critical Section 내부에서 즉시 처리)
            // HashMap의 Thread-Safety 문제를 방지하기 위해 저장까지 한 번에 처리
            UserPoint savedUserPoint = userPointRepository.save(userId, updatedUserPoint.point());

            // 4. 도메인 정적 팩토리 메서드로 포인트 내역 생성 및 저장
            PointHistory useHistory = PointHistory.createUse(0, userId, amount, savedUserPoint.updateMillis());
            pointHistoryRepository.save(useHistory);

            log.info("Successfully used {} points for userId: {}. New balance: {}",
                    amount, userId, savedUserPoint.point());

            // 저장된 실제 값을 반환하여 데이터 일관성 보장
            return savedUserPoint;
        } catch (Exception e) {
            log.error("Failed to use {} points for userId: {}. Error: {}", amount, userId, e.getMessage());
            throw e;
        } finally {
            userLock.unlock();
        }
    }
}
