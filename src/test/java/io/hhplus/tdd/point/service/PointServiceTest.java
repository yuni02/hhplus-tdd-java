package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * PointService 테스트
 * 
 * 테스트 범위:
 * - 서비스 레이어의 비즈니스 로직 흐름 검증
 * - Repository와의 협력 검증
 * - 도메인 객체 간의 상호작용 검증
 * 
 * 주의사항:
 * - 도메인 객체 내부 검증 로직은 각 도메인 테스트에서 담당
 * - 여기서는 서비스 로직과 레포지토리 협력에 집중
 */
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("유저 포인트 조회 - 성공")
    void getUserPoint_Success() {
        // given
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(userPointRepository.findById(userId)).willReturn(userPoint);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
        verify(userPointRepository).findById(userId);
    }

    /**
     * 포인트 충전 성공 테스트
     * 
     * 테스트 이유:
     * - 충전의 핵심 비즈니스 플로우 검증
     * - Repository 간의 올바른 협력 확인
     * - 도메인 로직과 인프라 계층의 통합 검증
     */
    @Test
    @DisplayName("포인트 충전 - 성공")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long amount = 500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 1500L)).willReturn(expectedUserPoint);
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then - 비즈니스 로직 결과 검증
        assertThat(result.point()).isEqualTo(existingUserPoint.point() + amount);

        // Repository 협력 검증
        verify(userPointRepository).findById(userId);
        verify(userPointRepository).save(userId, 1500L);

        // ArgumentCaptor로 히스토리 저장 검증
        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());

        PointHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.type()).isEqualTo(TransactionType.CHARGE);
        assertThat(savedHistory.amount()).isEqualTo(amount);
    }

    /**
     * 포인트 사용 성공 테스트
     * 
     * 테스트 이유:
     * - 사용의 핵심 비즈니스 플로우 검증
     * - 잔고 차감 계산의 정확성 확인
     * - Repository 간의 올바른 협력 확인
     */
    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long amount = 300L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 700L)).willReturn(
                new UserPoint(userId, 700L, System.currentTimeMillis()));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.USE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then - 비즈니스 로직 결과 검증
        assertThat(result.point()).isEqualTo(700L);

        // Repository 협력 검증
        verify(userPointRepository).findById(userId);
        verify(userPointRepository).save(userId, 700L);

        // ArgumentCaptor로 히스토리 저장 검증
        ArgumentCaptor<PointHistory> historyCaptor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(historyCaptor.capture());

        PointHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.type()).isEqualTo(TransactionType.USE);
        assertThat(savedHistory.amount()).isEqualTo(amount);
    }

    /**
     * 잘못된 금액으로 포인트 사용 실패 테스트 (0원, 음수 통합)
     * 
     * 테스트 이유:
     * - 서비스 레이어에서 도메인 검증이 올바르게 작동하는지 확인
     * - 유효하지 않은 입력에 대한 방어 로직 검증
     * 
     * 주의: 실제 validateAmount() 로직은 UserPointTest에서 상세 테스트
     */
    @Test
    @DisplayName("포인트 사용 - 잘못된 금액으로 실패")
    void usePoint_InvalidAmount_Fail() {
        // given
        long userId = 1L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);

        // when & then - 0원 테스트
        assertThatThrownBy(() -> pointService.usePoint(userId, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0보다 커야 합니다");

        // when & then - 음수 테스트
        assertThatThrownBy(() -> pointService.usePoint(userId, -100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("금액은 0보다 커야 합니다");
    }

    /**
     * 잔고 부족으로 포인트 사용 실패 테스트
     * 
     * 테스트 이유:
     * - 핵심 비즈니스 규칙(잔고 확인) 검증
     * - 서비스 레이어에서 도메인 규칙이 올바르게 적용되는지 확인
     */
    @Test
    @DisplayName("포인트 사용 - 잔고 부족으로 실패")
    void usePoint_InsufficientBalance_Fail() {
        // given
        long userId = 1L;
        long amount = 1500L; // 잔고보다 많은 금액
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고가 부족합니다");

        verify(userPointRepository).findById(userId);
        // 실패 시에는 save가 호출되지 않아야 함
        verify(userPointRepository, never()).save(anyLong(), anyLong());
        verify(pointHistoryRepository, never()).save(any(PointHistory.class));
    }

    /**
     * 정확히 잔고만큼 사용하는 경계값 테스트
     * 
     * 테스트 이유:
     * - 경계값에서의 비즈니스 로직 정확성 검증
     * - 정확한 잔고 계산 로직 확인
     */
    @Test
    @DisplayName("정확히 잔고만큼 사용 - 성공")
    void usePoint_ExactBalance_Success() {
        // given
        long userId = 1L;
        long balance = 1000L;
        long amount = 1000L; // 정확히 같은 금액
        UserPoint existingUserPoint = new UserPoint(userId, balance, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 0L)).willReturn(
                new UserPoint(userId, 0L, System.currentTimeMillis()));
        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.USE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertThat(result.point()).isEqualTo(0L); // 정확히 0원이 되어야 함
        verify(userPointRepository).save(userId, 0L);
    }

    @Test
    @DisplayName("포인트 내역 조회 - 성공")
    void getPointHistory_Success() {
        // given
        long userId = 1L;
        List<PointHistory> histories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 300L, TransactionType.USE, System.currentTimeMillis()));
        given(pointHistoryRepository.findAllByUserId(userId)).willReturn(histories);

        // when
        List<PointHistory> result = pointService.getPointHistory(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
        verify(pointHistoryRepository).findAllByUserId(userId);
    }
}