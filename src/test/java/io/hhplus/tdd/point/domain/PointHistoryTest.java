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

/**
 * PointHistory 도메인 객체 테스트
 * 
 * 테스트 목적:
 * 1. 포인트 거래 히스토리의 생성 및 검증 로직 확인
 * 2. 도메인 객체의 불변성과 일관성 보장
 * 3. 비즈니스 규칙 준수 여부 검증
 * 4. 예외 상황에 대한 적절한 처리 확인
 */
public class PointHistoryTest {

    @Nested
    @DisplayName("PointHistory 생성 테스트")
    class CreatePointHistoryTest {

        /**
         * 정상적인 충전 히스토리 생성 테스트
         * 
         * 테스트 이유:
         * - 가장 기본적인 성공 케이스 검증
         * - 정적 팩토리 메서드가 올바르게 작동하는지 확인
         * - 입력값이 그대로 저장되는지 검증 (데이터 무결성)
         */
        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 성공")
        void createPointHistory_Success() {
            // given - 유효한 입력 데이터 준비
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when - 충전 히스토리 생성
            PointHistory pointHistory = PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC));

            // then - 생성된 객체의 값이 정확한지 확인
            assertThat(pointHistory.userId()).isEqualTo(userId);
            assertThat(pointHistory.amount()).isEqualTo(amount);
        }

        /**
         * 정상적인 사용 히스토리 생성 테스트
         * 
         * 테스트 이유:
         * - 사용 타입의 히스토리 생성 검증
         * - isUse(), isCharge() 메서드의 정확성 확인
         * - 충전과 사용의 구분이 올바른지 검증
         */
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

            // then - 사용 타입 특화 검증
            assertThat(pointHistory.userId()).isEqualTo(userId);
            assertThat(pointHistory.amount()).isEqualTo(amount);
            assertThat(pointHistory.isUse()).isTrue();
            assertThat(pointHistory.isCharge()).isFalse();
        }

        /**
         * 잘못된 유저 ID로 충전 히스토리 생성 실패 테스트
         * 
         * 테스트 이유:
         * - 도메인 객체의 불변 조건 검증 (유저 ID > 0)
         * - 비즈니스 규칙 위반 시 적절한 예외 발생 확인
         * - 방어적 프로그래밍 원칙 적용 검증
         * - 데이터 무결성 보장
         */
        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 유저 ID 0 실패")
        void createPointHistory_Charge_Fail() {
            // given - 잘못된 유저 ID (0)
            long userId = 0L;
            long amount = 1000L;
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then - 예외 발생 검증
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유저 ID는 0보다 커야 합니다");
        }

        /**
         * 잘못된 유저 ID로 사용 히스토리 생성 실패 테스트
         * 
         * 테스트 이유:
         * - 충전과 사용 모두 동일한 유저 ID 검증 규칙 적용 확인
         * - 일관된 도메인 규칙 적용 검증
         */
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

        /**
         * 0원 충전 시도 실패 테스트
         * 
         * 테스트 이유:
         * - 비즈니스적으로 의미 없는 거래 방지
         * - 최소 거래 단위 규칙 적용 (1원 이상)
         * - 시스템 리소스 낭비 방지
         * - 거래 로그의 품질 보장
         */
        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 거래 금액 0원 실패")
        void createPointHistory_Charge_ZeroAmount_Fail() {
            // given
            long userId = 1L;
            long amount = 0L; // 잘못된 금액
            long transactionId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount,
                    createdAt.toEpochSecond(ZoneOffset.UTC)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 금액은 0보다 커야 합니다");
        }

        /**
         * 0원 사용 시도 실패 테스트
         * 
         * 테스트 이유:
         * - 충전과 동일한 금액 검증 규칙 적용 확인
         * - 도메인 규칙의 일관성 검증
         */
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

        /**
         * 잘못된 거래 시간으로 충전 히스토리 생성 실패 테스트
         * 
         * 테스트 이유:
         * - 거래 시간의 유효성 검증 (0은 유효하지 않은 타임스탬프)
         * - 데이터 추적성과 감사(Audit) 기능 보장
         * - 시간 기반 분석 및 리포트 품질 보장
         * - 시계열 데이터 무결성 확보
         */
        @Test
        @DisplayName("PointHistory 생성 중 충전할 경우 - 거래 시간 0 실패")
        void createPointHistory_Charge_ZeroTime_Fail() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            long invalidTime = 0L; // 잘못된 타임스탬프

            // when & then
            assertThatThrownBy(() -> PointHistory.createCharge(transactionId, userId, amount, invalidTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 시간은 유효해야 합니다");
        }

        /**
         * 잘못된 거래 시간으로 사용 히스토리 생성 실패 테스트
         * 
         * 테스트 이유:
         * - 충전과 동일한 시간 검증 규칙 적용 확인
         * - 모든 거래 타입에 대한 일관된 시간 검증
         */
        @Test
        @DisplayName("PointHistory 생성 중 사용할 경우 - 거래 시간 0 실패")
        void createPointHistory_Use_ZeroTime_Fail() {
            // given
            long userId = 1L;
            long amount = 1000L;
            long transactionId = 1L;
            long invalidTime = 0L;

            // when & then
            assertThatThrownBy(() -> PointHistory.createUse(transactionId, userId, amount, invalidTime))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("거래 시간은 유효해야 합니다");
        }
    }

    @Nested
    @DisplayName("PointHistory 거래 유형 테스트")
    class TransactionTypeTest {

        /**
         * 충전 거래 유형 확인 테스트
         * 
         * 테스트 이유:
         * - 거래 유형 판별 로직의 정확성 검증
         * - 비즈니스 로직에서 거래 유형별 다른 처리가 필요하므로 중요
         * - isCharge() 메서드의 신뢰성 확보
         */
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

            // then - 충전 타입 확인
            assertThat(pointHistory.isCharge()).isTrue();
        }

        /**
         * 사용 거래 유형 확인 테스트
         * 
         * 테스트 이유:
         * - 사용 거래 유형 판별의 정확성 검증
         * - 충전과 사용이 명확히 구분되는지 확인 (상호 배타적)
         * - 조건문 로직의 완전성 검증
         */
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

            // then - 사용 타입 확인 및 충전이 아님을 확인
            assertThat(pointHistory.isUse()).isTrue();
            assertThat(pointHistory.isCharge()).isFalse();
        }
    }

    @Nested
    @DisplayName("PointHistory 거래 금액 테스트")
    class AmountTest {

        /**
         * 충전 거래 금액 저장 확인 테스트
         * 
         * 테스트 이유:
         * - 입력된 금액이 정확히 저장되는지 검증
         * - 데이터 손실이나 변환 오류 방지
         * - 금융 거래에서 금액 정확성은 매우 중요
         */
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

            // then - 입력 금액과 저장된 금액이 일치하는지 확인
            assertThat(pointHistory.amount()).isEqualTo(amount);
        }

        /**
         * 사용 거래 금액 저장 확인 테스트
         * 
         * 테스트 이유:
         * - 사용 거래에서도 금액이 정확히 저장되는지 검증
         * - 충전과 사용 모두 동일한 금액 저장 방식 확인
         */
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

        /**
         * 충전 거래 시간 저장 확인 테스트
         * 
         * 테스트 이유:
         * - 거래 시간이 정확히 저장되는지 검증
         * - 감사(Audit) 로그 기능의 신뢰성 확보
         * - 시간 기반 분석 및 리포트 정확성 보장
         */
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

            // then - 입력 시간과 저장된 시간이 일치하는지 확인
            assertThat(pointHistory.updateMillis()).isEqualTo(createdAt.toEpochSecond(ZoneOffset.UTC));
        }

        /**
         * 사용 거래 시간 저장 확인 테스트
         * 
         * 테스트 이유:
         * - 사용 거래에서도 시간이 정확히 저장되는지 검증
         * - 모든 거래 타입에 대한 일관된 시간 처리 확인
         */
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

        /**
         * 충전 시 포인트 변화량 확인 테스트
         * 
         * 테스트 이유:
         * - 포인트 잔고 계산에 사용되는 핵심 비즈니스 로직 검증
         * - 충전은 양수(+) 변화를 가져야 함
         * - getPointChange() 메서드의 정확성 확보
         * - 잔고 계산 오류 방지 (매우 중요한 금융 로직)
         */
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

            // then - 충전은 양수 변화량을 가져야 함
            assertThat(pointHistory.getPointChange()).isEqualTo(amount);
        }

        /**
         * 사용 시 포인트 변화량 확인 테스트
         * 
         * 테스트 이유:
         * - 사용은 음수(-) 변화를 가져야 함을 검증
         * - 포인트 잔고 감소 로직의 정확성 확보
         * - 충전과 사용의 변화량 부호가 반대임을 확인
         * - 회계 원리(차변/대변) 적용 검증
         */
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

            // then - 사용은 음수 변화량을 가져야 함
            assertThat(pointHistory.getPointChange()).isEqualTo(-amount);
        }
    }
}
