package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("유저 포인트 조회 - 성공")
    void getUserPoint_Success() {
        // given
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(userPointTable.selectById(userId)).willReturn(userPoint);

        // when
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1000L);
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 성공")
    void chargePoint_Success() {
        // given
        long userId = 1L;
        long amount = 500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());
        
        given(userPointTable.selectById(userId)).willReturn(existingUserPoint);
        given(userPointTable.insertOrUpdate(userId, 1500L)).willReturn(expectedUserPoint);
        given(pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, anyLong()))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.CHARGE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.chargePoint(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(1500L);
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 1500L);
        verify(pointHistoryTable).insert(userId, amount, TransactionType.CHARGE, anyLong());
    }

    @Test
    @DisplayName("포인트 사용 - 성공")
    void usePoint_Success() {
        // given
        long userId = 1L;
        long amount = 300L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        UserPoint expectedUserPoint = new UserPoint(userId, 700L, System.currentTimeMillis());
        
        given(userPointTable.selectById(userId)).willReturn(existingUserPoint);
        given(userPointTable.insertOrUpdate(userId, 700L)).willReturn(expectedUserPoint);
        given(pointHistoryTable.insert(userId, amount, TransactionType.USE, anyLong()))
                .willReturn(new PointHistory(1L, userId, amount, TransactionType.USE, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.usePoint(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(700L);
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(userId, 700L);
        verify(pointHistoryTable).insert(userId, amount, TransactionType.USE, anyLong());
    }

    @Test
    @DisplayName("잔고 부족으로 포인트 사용 실패")
    void usePoint_InsufficientBalance_Fail() {
        // given
        long userId = 1L;
        long amount = 1500L;
        UserPoint existingUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        
        given(userPointTable.selectById(userId)).willReturn(existingUserPoint);

        // when & then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고가 부족합니다");
        
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 - 성공")
    void getPointHistory_Success() {
        // given
        long userId = 1L;
        List<PointHistory> histories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 300L, TransactionType.USE, System.currentTimeMillis())
        );
        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(histories);

        // when
        List<PointHistory> result = pointService.getPointHistory(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.CHARGE);
        assertThat(result.get(0).amount()).isEqualTo(1000L);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.USE);
        assertThat(result.get(1).amount()).isEqualTo(300L);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("포인트 충전 - 음수 금액으로 실패")
    void chargePoint_NegativeAmount_Fail() {
        // given
        long userId = 1L;
        long amount = -100L;

        // when & then
        assertThatThrownBy(() -> pointService.chargePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다");
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
} 