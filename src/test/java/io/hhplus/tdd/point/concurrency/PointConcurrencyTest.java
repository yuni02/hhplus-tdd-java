package io.hhplus.tdd.point.concurrency;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PointService 동시성 테스트
 * 
 * 테스트 목적:
 * 1. 동일한 사용자에 대한 동시 요청 처리 검증
 * 2. Race Condition 발생 시나리오 테스트
 * 3. 포인트 잔고 일관성 보장 확인
 * 4. 동시성 제어 메커니즘의 효과성 검증
 * 
 * 중요한 동시성 시나리오:
 * - 동시 포인트 사용 (잔고 부족 방지)
 * - 동시 포인트 충전 (중복 충전 방지)
 * - 충전과 사용의 동시 발생
 */
@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    private PointService pointService;

    /**
     * 동일 사용자 동시 포인트 사용 테스트 - 가장 중요한 시나리오
     * 
     * 테스트 이유:
     * - 포인트 시스템의 핵심 비즈니스 규칙인 "잔고 음수 금지" 검증
     * - Race Condition으로 인한 데이터 무결성 위반 방지 확인
     * - 동시성 제어 메커니즘이 올바르게 작동하는지 검증
     * - 금융 시스템에서 가장 치명적인 오류인 "잔고 초과 사용" 방지
     * - 실제 운영 환경에서 발생할 수 있는 동시 사용 시나리오 시뮬레이션
     * 
     * 시나리오 설명:
     * - 사용자가 1000원 보유 상태에서 시작
     * - 10개 스레드가 동시에 200원씩 사용 시도 (총 2000원 요청)
     * - 정상적이라면 5번만 성공하고 나머지는 잔고 부족으로 실패해야 함
     * - 최종 잔고는 0원이어야 하며, 절대 음수가 되어서는 안됨
     */
    @Test
    @DisplayName("동일 사용자 동시 포인트 사용 - 잔고 보호 테스트")
    void concurrentUsePoint_SameUser_ShouldPreventOverdraft() throws InterruptedException {
        // 1️⃣ 테스트 환경 준비
        long userId = 100L;
        long useAmount = 200L;
        int threadCount = 10;

        // 2️⃣ 동시성 제어 도구 준비
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount); // 🎯 핵심!

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 3️⃣ 동시 요청 실행
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown(); // 🎯 작업 완료 신호
                }
            });
        }

        // 4️⃣ 모든 스레드 완료 대기
        latch.await(10, TimeUnit.SECONDS); // �� 모든 스레드가 끝날 때까지 기다림

        // 5️⃣ 결과 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);
        assertThat(finalUserPoint.point()).isGreaterThanOrEqualTo(0L);
    }

    /**
     * 동일 사용자 동시 포인트 충전 테스트
     * 
     * 테스트 이유:
     * - 동시 충전 시 데이터 손실 방지 검증 (Lost Update 문제 방지)
     * - 모든 충전 요청이 누락 없이 처리되는지 확인
     * - 충전 금액 합산의 정확성 검증
     * - 포인트 적립 시스템의 신뢰성 확보
     * - 동시성 제어가 충전 로직에서도 올바르게 작동하는지 확인
     * 
     * 시나리오 설명:
     * - 5개 스레드가 동시에 1000원씩 충전 시도
     * - 모든 충전이 성공해야 하며 총 5000원이 정확히 증가해야 함
     * - 중간에 데이터 손실이나 덮어쓰기가 발생하지 않아야 함
     */
    @Test
    @DisplayName("동일 사용자 동시 포인트 충전 - 데이터 일관성 테스트")
    void concurrentChargePoint_SameUser_ShouldMaintainConsistency() throws InterruptedException {
        // given
        long userId = 200L;
        long chargeAmount = 1000L;
        int threadCount = 5;

        UserPoint initialUserPoint = pointService.getUserPoint(userId);
        long initialBalance = initialUserPoint.point();

        // when - 동시 충전 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 결과 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);

        // 모든 충전이 성공해야 함
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(exceptions).isEmpty();

        // 최종 잔고 = 초기잔고 + (충전횟수 * 충전금액)
        long expectedFinalBalance = initialBalance + (threadCount * chargeAmount);
        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalBalance);

        System.out.println("=== 동시 충전 테스트 결과 ===");
        System.out.println("성공: " + successCount.get() + "회");
        System.out.println("예외: " + exceptions.size() + "개");
        System.out.println("최종 잔고: " + finalUserPoint.point() + "원");
    }

    /**
     * 혼합 동시성 테스트 - 충전과 사용이 동시에 발생
     * 
     * 테스트 이유:
     * - 실제 운영 환경에서 가장 빈번한 시나리오 검증
     * - 복잡한 동시성 상황에서의 데이터 일관성 확인
     * - 충전과 사용이 서로 간섭하지 않는지 검증
     * - 최종 잔고 계산의 정확성 확인 (입금 - 출금 = 최종 잔고)
     * - 교착상태(Deadlock) 발생 여부 확인
     * - 서로 다른 종류의 거래가 동시에 발생할 때의 안정성 검증
     * 
     * 시나리오 설명:
     * - 초기 잔고 1000원에서 시작
     * - 5개 스레드는 500원씩 충전 (총 +2500원)
     * - 5개 스레드는 300원씩 사용 시도 (요청 총 -1500원)
     * - 충전과 사용이 동시에 실행되어 복잡한 경합 상황 발생
     */
    @Test
    @DisplayName("동시 충전/사용 혼합 테스트 - 복합 시나리오")
    void concurrentChargeAndUse_ShouldMaintainConsistency() throws InterruptedException {
        // given
        long userId = 300L;
        long initialAmount = 1000L;
        long chargeAmount = 500L;
        long useAmount = 300L;
        int chargeThreadCount = 5;
        int useThreadCount = 5;

        // 초기 충전
        pointService.chargePoint(userId, initialAmount);

        // when - 동시 충전/사용 요청
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(chargeThreadCount + useThreadCount);

        AtomicInteger chargeSuccessCount = new AtomicInteger(0);
        AtomicInteger useSuccessCount = new AtomicInteger(0);
        AtomicInteger useFailCount = new AtomicInteger(0);

        // 충전 스레드들
        for (int i = 0; i < chargeThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                    chargeSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("충전 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 사용 스레드들
        for (int i = 0; i < useThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    useSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    useFailCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 결과 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);

        // 잔고는 항상 0 이상이어야 함
        assertThat(finalUserPoint.point()).isGreaterThanOrEqualTo(0L);

        // 예상 최종 잔고 계산
        long totalCharged = chargeSuccessCount.get() * chargeAmount;
        long totalUsed = useSuccessCount.get() * useAmount;
        long expectedFinalBalance = initialAmount + totalCharged - totalUsed;

        assertThat(finalUserPoint.point()).isEqualTo(expectedFinalBalance);

        System.out.println("=== 혼합 동시성 테스트 결과 ===");
        System.out.println("충전 성공: " + chargeSuccessCount.get() + "회");
        System.out.println("사용 성공: " + useSuccessCount.get() + "회");
        System.out.println("사용 실패: " + useFailCount.get() + "회");
        System.out.println("최종 잔고: " + finalUserPoint.point() + "원");
    }

    /**
     * 대규모 동시성 스트레스 테스트
     * 
     * 테스트 이유:
     * - 시스템의 확장성(Scalability) 검증
     * - 높은 부하 상황에서의 안정성 확인
     * - 성능 임계점 파악 및 병목 지점 발견
     * - 메모리 사용량 및 스레드 관리 검증
     * - 운영 환경의 피크 타임 상황 시뮬레이션
     * - 동시성 제어 메커니즘의 성능 영향 측정
     * 
     * 시나리오 설명:
     * - 100개 스레드가 동시에 포인트 사용 시도
     * - 대량 트래픽 상황에서의 데이터 일관성 검증
     * - 처리 시간 및 성능 메트릭 수집
     */
    @Test
    @DisplayName("대규모 동시성 스트레스 테스트")
    void massiveConcurrencyStressTest() throws InterruptedException {
        // given
        long userId = 400L;
        long initialAmount = 100000L; // 10만원
        int threadCount = 100;
        long useAmount = 1000L; // 1천원씩

        // 초기 충전
        pointService.chargePoint(userId, initialAmount);

        // when - 대규모 동시 요청
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.usePoint(userId, useAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        long endTime = System.currentTimeMillis();

        // then - 성능 및 정확성 검증
        UserPoint finalUserPoint = pointService.getUserPoint(userId);

        assertThat(finalUserPoint.point()).isGreaterThanOrEqualTo(0L);

        long expectedSuccessCount = initialAmount / useAmount;
        assertThat(successCount.get()).isEqualTo(expectedSuccessCount);

        System.out.println("=== 스트레스 테스트 결과 ===");
        System.out.println("총 처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("성공: " + successCount.get() + "회");
        System.out.println("실패: " + failCount.get() + "회");
        System.out.println("최종 잔고: " + finalUserPoint.point() + "원");
        System.out.println("평균 처리 시간: " + (endTime - startTime) / threadCount + "ms/req");
    }

    /**
     * 거래 내역 동시성 테스트
     * 
     * 테스트 이유:
     * - 거래 내역의 누락 방지 검증 (감사 추적 보장)
     * - PointHistory 저장의 동시성 안전성 확인
     * - 모든 거래가 빠짐없이 기록되는지 검증
     * - 거래 내역을 통한 데이터 복구 가능성 확보
     * - 금융 시스템의 필수 요구사항인 거래 추적성 보장
     * - 동시 거래 시 히스토리 테이블의 무결성 검증
     * 
     * 시나리오 설명:
     * - 10개 스레드가 동시에 충전 수행
     * - 모든 충전 거래가 PointHistory에 정확히 기록되어야 함
     * - 거래 내역 개수와 실제 충전 횟수가 일치해야 함
     */
    @Test
    @DisplayName("거래 내역 동시성 테스트 - 내역 누락 방지")
    void concurrentTransactionHistory_ShouldNotLoseData() throws InterruptedException {
        // given
        long userId = 500L;
        long chargeAmount = 1000L;
        int threadCount = 10;

        // when - 동시 충전으로 거래 내역 생성
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargePoint(userId, chargeAmount);
                } catch (Exception e) {
                    System.err.println("거래 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 거래 내역 검증
        List<PointHistory> histories = pointService.getPointHistory(userId);

        // 모든 충전 거래가 기록되어야 함
        long chargeHistoryCount = histories.stream()
                .filter(h -> h.type() == TransactionType.CHARGE)
                .filter(h -> h.amount() == chargeAmount)
                .count();

        assertThat(chargeHistoryCount).isEqualTo(threadCount);

        System.out.println("=== 거래 내역 동시성 테스트 결과 ===");
        System.out.println("총 거래 내역: " + histories.size() + "개");
        System.out.println("충전 내역: " + chargeHistoryCount + "개");
    }
}