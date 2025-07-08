package io.hhplus.tdd.point.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import static org.assertj.core.api.Assertions.*;

class UserPointTest {

    @Nested
    @DisplayName("UserPoint 생성 테스트")
    class CreateUserPointTest {

        @Test
        @DisplayName("빈 UserPoint 생성 - 성공")
        void createEmptyUserPoint_Success() {
            // when
            UserPoint userPoint = UserPoint.empty(1L);

            // then
            assertThat(userPoint.id()).isEqualTo(1L);
            assertThat(userPoint.point()).isEqualTo(0L);
            assertThat(userPoint.updateMillis()).isGreaterThan(0L);
        }

        @Test
        @DisplayName("UserPoint 직접 생성 - 성공")
        void createUserPoint_Success() {
            // given
            long currentTime = System.currentTimeMillis();

            // when
            UserPoint userPoint = new UserPoint(1L, 1000L, currentTime);

            // then
            assertThat(userPoint.id()).isEqualTo(1L);
            assertThat(userPoint.point()).isEqualTo(1000L);
            assertThat(userPoint.updateMillis()).isEqualTo(currentTime);
        }
    }

    @Nested
    @DisplayName("금액 유효성 검증 테스트")
    class ValidateAmountTest {

        @Test
        @DisplayName("정상 금액 검증 - 성공")
        void validateAmount_ValidAmount_Success() {
            // when & then
            assertThatNoException().isThrownBy(() -> UserPoint.validateAmount(1L));
            assertThatNoException().isThrownBy(() -> UserPoint.validateAmount(1000L));
            assertThatNoException().isThrownBy(() -> UserPoint.validateAmount(999999L));
        }

        @Test
        @DisplayName("0원 금액 검증 - 실패")
        void validateAmount_ZeroAmount_Fail() {
            // when & then
            assertThatThrownBy(() -> UserPoint.validateAmount(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");
        }

        @Test
        @DisplayName("음수 금액 검증 - 실패")
        void validateAmount_NegativeAmount_Fail() {
            // when & then
            assertThatThrownBy(() -> UserPoint.validateAmount(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");

            assertThatThrownBy(() -> UserPoint.validateAmount(-1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("잔고 유효성 검증 테스트")
    class ValidateBalanceTest {

        @Test
        @DisplayName("정상 잔고 검증 - 성공")
        void validateBalance_ValidBalance_Success() {
            // given
            UserPoint userPoint1 = new UserPoint(1L, 0L, System.currentTimeMillis());
            UserPoint userPoint2 = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThatNoException().isThrownBy(userPoint1::validateBalance);
            assertThatNoException().isThrownBy(userPoint2::validateBalance);
        }

        @Test
        @DisplayName("음수 잔고 검증 - 실패")
        void validateBalance_NegativeBalance_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, -1L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(userPoint::validateBalance)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("포인트 잔고는 음수가 될 수 없습니다");
        }
    }

    @Nested
    @DisplayName("포인트 사용 가능 여부 테스트")
    class CanUseTest {

        @Test
        @DisplayName("충분한 잔고로 사용 가능 - 성공")
        void canUse_SufficientBalance_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThat(userPoint.canUse(500L)).isTrue();
            assertThat(userPoint.canUse(1000L)).isTrue(); // 정확히 잔고만큼
            assertThat(userPoint.canUse(1L)).isTrue();
        }

        @Test
        @DisplayName("잔고 부족으로 사용 불가 - 실패")
        void canUse_InsufficientBalance_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThat(userPoint.canUse(1001L)).isFalse();
            assertThat(userPoint.canUse(2000L)).isFalse();
        }

        @Test
        @DisplayName("잔고 0원에서 사용 불가")
        void canUse_ZeroBalance_Fail() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);

            // when & then
            assertThat(userPoint.canUse(1L)).isFalse();
        }

        @Test
        @DisplayName("잘못된 사용 금액 - 실패")
        void canUse_InvalidAmount_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(() -> userPoint.canUse(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");

            assertThatThrownBy(() -> userPoint.canUse(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("포인트 충전 테스트")
    class ChargeTest {

        @Test
        @DisplayName("정상 포인트 충전 - 성공")
        void charge_ValidAmount_Success() throws InterruptedException {
            // given
            UserPoint originalPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
            Thread.sleep(1);

            // when
            UserPoint chargedPoint = originalPoint.charge(500L);

            // then
            assertThat(chargedPoint.id()).isEqualTo(1L);
            assertThat(chargedPoint.point()).isEqualTo(1500L);
            assertThat(chargedPoint.updateMillis()).isGreaterThan(originalPoint.updateMillis());

            // 불변성 확인
            assertThat(originalPoint.point()).isEqualTo(1000L); // 원본 객체는 변경되지 않음
        }

        @Test
        @DisplayName("0원에서 포인트 충전 - 성공")
        void charge_FromZero_Success() {
            // given
            UserPoint emptyPoint = UserPoint.empty(1L);

            // when
            UserPoint chargedPoint = emptyPoint.charge(1000L);

            // then
            assertThat(chargedPoint.point()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("잘못된 충전 금액 - 실패")
        void charge_InvalidAmount_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(() -> userPoint.charge(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");

            assertThatThrownBy(() -> userPoint.charge(-1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("포인트 사용 테스트")
    class UseTest {

        @Test
        @DisplayName("정상 포인트 사용 - 성공")
        void use_ValidAmount_Success() throws InterruptedException {
            // given
            UserPoint originalPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());
            Thread.sleep(1);

            // when
            UserPoint usedPoint = originalPoint.use(300L);

            // then
            assertThat(usedPoint.id()).isEqualTo(1L);
            assertThat(usedPoint.point()).isEqualTo(700L);
            assertThat(usedPoint.updateMillis()).isGreaterThan(originalPoint.updateMillis());

            // 불변성 확인
            assertThat(originalPoint.point()).isEqualTo(1000L); // 원본 객체는 변경되지 않음
        }

        @Test
        @DisplayName("정확히 잔고만큼 사용 - 성공")
        void use_ExactBalance_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when
            UserPoint usedPoint = userPoint.use(1000L);

            // then
            assertThat(usedPoint.point()).isEqualTo(0L);
        }

        @Test
        @DisplayName("잔고 부족으로 사용 실패")
        void use_InsufficientBalance_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(() -> userPoint.use(1001L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잔고가 부족합니다");

            assertThatThrownBy(() -> userPoint.use(2000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잔고가 부족합니다");
        }

        @Test
        @DisplayName("0원 잔고에서 사용 실패")
        void use_ZeroBalance_Fail() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);

            // when & then
            assertThatThrownBy(() -> userPoint.use(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("잔고가 부족합니다");
        }

        @Test
        @DisplayName("잘못된 사용 금액 - 실패")
        void use_InvalidAmount_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(() -> userPoint.use(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");

            assertThatThrownBy(() -> userPoint.use(-500L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("금액은 0보다 커야 합니다");
        }
    }

    @Nested
    @DisplayName("충전 가능 여부 테스트")
    class CanChargeTest {

        @Test
        @DisplayName("한도 내 충전 가능 - 성공")
        void canCharge_WithinLimit_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 500000L, System.currentTimeMillis());

            // when & then
            assertThat(userPoint.canCharge(400000L)).isTrue();
            assertThat(userPoint.canCharge(500000L)).isTrue(); // 정확히 한도까지
        }

        @Test
        @DisplayName("한도 초과 충전 불가 - 실패")
        void canCharge_OverLimit_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 500000L, System.currentTimeMillis());

            // when & then
            assertThat(userPoint.canCharge(500001L)).isFalse();
            assertThat(userPoint.canCharge(1000000L)).isFalse();
        }

        @Test
        @DisplayName("최대 한도에서 추가 충전 불가")
        void canCharge_AtMaxLimit_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1000000L, System.currentTimeMillis());

            // when & then
            assertThat(userPoint.canCharge(1L)).isFalse();
        }
    }

    @Nested
    @DisplayName("안전한 포인트 충전 테스트")
    class SafeChargeTest {

        @Test
        @DisplayName("한도 내 안전한 충전 - 성공")
        void safeCharge_WithinLimit_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 500000L, System.currentTimeMillis());

            // when
            UserPoint chargedPoint = userPoint.safeCharge(400000L);

            // then
            assertThat(chargedPoint.point()).isEqualTo(900000L);
        }

        @Test
        @DisplayName("한도 초과 안전한 충전 - 실패")
        void safeCharge_OverLimit_Fail() {
            // given
            UserPoint userPoint = new UserPoint(1L, 500000L, System.currentTimeMillis());

            // when & then
            assertThatThrownBy(() -> userPoint.safeCharge(500001L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("포인트 충전 한도를 초과했습니다");
        }
    }

    @Nested
    @DisplayName("연속 거래 테스트")
    class ChainedTransactionTest {

        @Test
        @DisplayName("충전 후 사용 - 성공")
        void chargeAndUse_Success() {
            // given
            UserPoint originalPoint = UserPoint.empty(1L);

            // when
            UserPoint chargedPoint = originalPoint.charge(1000L);
            UserPoint usedPoint = chargedPoint.use(300L);

            // then
            assertThat(usedPoint.point()).isEqualTo(700L);
            assertThat(originalPoint.point()).isEqualTo(0L); // 불변성 확인
        }

        @Test
        @DisplayName("여러 번 충전 - 성공")
        void multipleCharges_Success() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);

            // when
            UserPoint result = userPoint
                    .charge(1000L)
                    .charge(2000L)
                    .charge(3000L);

            // then
            assertThat(result.point()).isEqualTo(6000L);
        }

        @Test
        @DisplayName("여러 번 사용 - 성공")
        void multipleUses_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 10000L, System.currentTimeMillis());

            // when
            UserPoint result = userPoint
                    .use(1000L)
                    .use(2000L)
                    .use(3000L);

            // then
            assertThat(result.point()).isEqualTo(4000L);
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryTest {

        @Test
        @DisplayName("최대 한도 충전 테스트")
        void chargeToMaxLimit_Success() {
            // given
            UserPoint userPoint = UserPoint.empty(1L);

            // when
            UserPoint chargedPoint = userPoint.charge(1000000L);

            // then
            assertThat(chargedPoint.point()).isEqualTo(1000000L);
            assertThat(chargedPoint.canCharge(1L)).isFalse();
        }

        @Test
        @DisplayName("1원 단위 거래 테스트")
        void oneWonTransaction_Success() {
            // given
            UserPoint userPoint = new UserPoint(1L, 1L, System.currentTimeMillis());

            // when
            UserPoint usedPoint = userPoint.use(1L);

            // then
            assertThat(usedPoint.point()).isEqualTo(0L);
        }
    }
}
