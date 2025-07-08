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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
        given(pointHistoryRepository.save(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong()))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1500L);
        verify(userPointRepository).findById(userId);
        verify(userPointRepository).save(userId, 1500L);
        verify(pointHistoryRepository).save(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long amount = 300L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, 700L, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 700L)).willReturn(expectedUserPoint);
        given(pointHistoryRepository.save(eq(userId), eq(amount), eq(TransactionType.USE), anyLong()))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.USE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(700L);
        verify(userPointRepository).findById(userId);
        verify(userPointRepository).save(userId, 700L);
        verify(pointHistoryRepository).save(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("잔고 부족으로 포인트 사용 실패") // 핵심 로직 중의 로직
    void usePoint_InsufficientBalance_Fail() {
        // given
        long userId = 1L;
        long amount = 1500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고가 부족합니다");

        verify(userPointRepository).findById(userId);
    }

    // 3. 경계값 테스트
    @Test
    @DisplayName("정확히 잔고만큼 사용 - 성공")
    void usePoint_ExactBalance_Success() {
        // given
        long userId = 1L;
        long balance = 1000L;
        long amount = 1000L; // 정확히 같은 금액
        UserPoint existingUserPoint = new UserPoint(userId, balance, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 0L)).willReturn(new UserPoint(userId, 0L, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertThat(result.point()).isEqualTo(0L); // 🔥 정확히 0원이 되어야 함
    }

    @Test
    @DisplayName("잔고보다 1원 많이 사용 - 실패")
    void usePoint_OneWonOver_Fail() {
        // given
        long userId = 1L;
        long balance = 1000L;
        long amount = 1001L; // 1원 초과
        UserPoint existingUserPoint = new UserPoint(userId, balance, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고가 부족합니다");
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
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(0).amount()).isEqualTo(1000L);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
        assertThat(result.get(1).amount()).isEqualTo(300L);
        verify(pointHistoryRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 사용 - 음수 금액으로 실패")
    void usePoint_NegativeAmount_Fail() {
        // given
        long userId = 1L;
        long amount = -100L;

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 충전 - 0원 충전으로 실패")
    void chargePoint_ZeroAmount_Fail() {
        // given
        long userId = 1L;
        long amount = 0L;

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 사용 - 0원 사용으로 실패")
    void usePoint_ZeroAmount_Fail() {
        // given
        long userId = 1L;
        long amount = 0L;

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용 금액은 0보다 커야 합니다");
    }

    @Test
    @DisplayName("포인트 충전 - 실제 비즈니스 로직 검증")
    void chargePoint_ShouldReturnUpdatedPoint() {
        // given
        long userId = 1L;
        long amount = 500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());

        given(userPointRepository.findById(userId)).willReturn(existingUserPoint);
        given(userPointRepository.save(userId, 1500L)).willReturn(expectedUserPoint);
        given(pointHistoryRepository.save(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong()))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        // 실제로 findById로 가져온 값에서 amount를 더한 값이 반환되는지 검증
        assertThat(result.point()).isEqualTo(existingUserPoint.point() + amount);

        // DB 업데이트가 올바른 값으로 호출되는지 검증
        verify(userPointRepository).save(userId, existingUserPoint.point() + amount);

        // 히스토리 저장이 올바른 값으로 호출되는지 검증
        verify(pointHistoryRepository).save(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
    }

    // 2. 동시성 테스트 (추가 필요)
    @Test
    @DisplayName("동시에 포인트 사용 시 잔고 검증")
    void usePoint_ConcurrentAccess_ShouldPreventOverdraft() {
        // 실제 프로덕션에서 가장 위험한 시나리오
    }

}