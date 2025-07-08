package io.hhplus.tdd.point.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.hhplus.tdd.point.domain.constant.TransactionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PointHistoryTest {

    @Nested
    @DisplayName("PointHistory 생성 테스트")
    class CreatePointHistoryTest {

        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 성공")
        void createPointHistory_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.userId()).isEqualTo(userId);
            assertThat(pointHistory.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("PointHistory 생성 중 사용할 경우 - 거래 금액 1000원 성공")
        void createPointHistory_Use_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.userId()).isEqualTo(userId);
            assertThat(pointHistory.amount()).isEqualTo(amount);
            assertThat(pointHistory.isUse()).isTrue();
            assertThat(pointHistory.isCharge()).isFalse();
        }

        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 유저 ID 0 실패")
        void createPointHistory_Charge_Fail() {
            // given
            long userId = 0L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유저 ID는 0보다 커야 합니다");

        }

        @Test
        @DisplayName("PointHistory 생성 중 사용할 경우 - 유저 ID 0 실패")
        void createPointHistory_Use_Fail() {
            // given
            long userId = 0L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유저 ID는 0보다 커야 합니다");
        }

        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 거래 금액 0원 실패")
        void createPointHistory_Charge_ZeroAmount_Fail() {
            // given
            long userId = 1L;
            long amount = 0L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("PointHistory 생성 중 사용할 경우 - 거래 금액 0원 실패")
        void createPointHistory_Use_ZeroAmount_Fail() {
            // given
            long userId = 1L;
            long amount = 0L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 거래 시간 0 실패")
        void createPointHistory_Charge_ZeroTime_Fail() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            long invalidTime = 0L; // 실제로 0을 전달

            // when & then
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount, invalidTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 시간은 유효해야 합니다");
        }

        @Test
        @DisplayName("PointHistory 생성 중 사용할 경우 - 거래 시간 0 실패")
        void createPointHistory_Use_ZeroTime_Fail() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            long invalidTime = 0L; // 실제로 0을 전달

            // when & then
            assertThatThrownBy(() -> PointHistory.createUse(transactionId, userId, amount, invalidTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 시간은 유효해야 합니다");
        }
    }

    @Nested
    @DisplayName("PointHistory 거래 유형 테스트")
    class TransactionTypeTest {

        @Test
        @DisplayName("PointHistory 거래 유형 - 충전")
        void transactionType_Charge_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.isCharge()).isTrue();
        }

        @Test
        @DisplayName("PointHistory 거래 유형 - 사용")
        void transactionType_Use_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.isUse()).isTrue();
            assertThat(pointHistory.isCharge()).isFalse();
        }

    }

    @Nested
    @DisplayName("PointHistory 거래 금액 테스트")
    class AmountTest {

        @Test
        @DisplayName("PointHistory 거래 금액 - 충전")
        void amount_Charge_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.amount()).isEqualTo(amount);
        }

        @Test
        @DisplayName("PointHistory 거래 금액 - 사용")
        void amount_Use_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.amount()).isEqualTo(amount);
        }
    }

    @Nested
    @DisplayName("PointHistory 거래 시간 테스트")
    class TimeTest {

        @Test
        @DisplayName("PointHistory 거래 시간 - 충전")
        void time_Charge_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.updateMillis()).isEqualTo(createdAt.toEpochSecond(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("PointHistory 거래 시간 - 사용")
        void time_Use_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.updateMillis()).isEqualTo(createdAt.toEpochSecond(ZoneOffset.UTC));
        }

    }

    @Nested
    @DisplayName("PointHistory 거래 유형에 따른 실제 포인트 변화량 테스트")
    class PointChangeTest {

        @Test
        @DisplayName("PointHistory 거래 유형에 따른 실제 포인트 변화량 - 충전")
        void pointChange_Charge_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.getPointChange()).isEqualTo(amount);
        }

        @Test
        @DisplayName("PointHistory 거래 유형에 따른 실제 포인트 변화량 - 사용")
        void pointChange_Use_Success() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when
            PointHistory pointHistory = PointHistory.createUse(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then
            assertThat(pointHistory.getPointChange()).isEqualTo(-amount);
        }

    }

}
