package io.hhplus.tdd.point.domain;

import io.hhplus.tdd.point.domain.constant.TransactionType;

public record PointHistory(
                long id,
                long userId,
                long amount,
                TransactionType type,
                long updateMillis) {

        /**
         * 포인트 히스토리 유효성 검증
         */
        public void validate() {
                if (userId <= 0) {
                        throw new IllegalArgumentException("유저 ID는 0보다 커야 합니다");
                }
                if (amount <= 0) {
                        throw new IllegalArgumentException("거래 금액은 0보다 커야 합니다");
                }
                if (type == null) {
                        throw new IllegalArgumentException("거래 유형은 필수입니다");
                }
                if (updateMillis <= 0) {
                        throw new IllegalArgumentException("거래 시간은 유효해야 합니다");
                }
        }

        /**
         * 충전 히스토리 생성 (정적 팩토리 메서드)
         */
        public static PointHistory createCharge(long id, long userId, long amount) {
                return createCharge(id, userId, amount, System.currentTimeMillis());
        }

        /**
         * 충전 히스토리 생성 (정적 팩토리 메서드 - 시간 지정)
         */
        public static PointHistory createCharge(long id, long userId, long amount, long updateMillis) {
                PointHistory history = new PointHistory(id, userId, amount, TransactionType.CHARGE, updateMillis);
                history.validate();
                return history;
        }

        /**
         * 사용 히스토리 생성 (정적 팩토리 메서드)
         */
        public static PointHistory createUse(long id, long userId, long amount) {
                return createUse(id, userId, amount, System.currentTimeMillis());
        }

        /**
         * 사용 히스토리 생성 (정적 팩토리 메서드 - 시간 지정)
         */
        public static PointHistory createUse(long id, long userId, long amount, long updateMillis) {
                PointHistory history = new PointHistory(id, userId, amount, TransactionType.USE, updateMillis);
                history.validate();
                return history;
        }

        /**
         * 충전 거래인지 확인
         */
        public boolean isCharge() {
                return TransactionType.CHARGE.equals(this.type);
        }

        /**
         * 사용 거래인지 확인
         */
        public boolean isUse() {
                return TransactionType.USE.equals(this.type);
        }

        /**
         * 특정 금액 이상의 거래인지 확인
         */
        public boolean isAmountGreaterThan(long compareAmount) {
                return this.amount > compareAmount;
        }

        /**
         * 특정 시간 이후의 거래인지 확인
         */
        public boolean isAfter(long timeMillis) {
                return this.updateMillis > timeMillis;
        }

        /**
         * 거래 유형에 따른 실제 포인트 변화량 반환 (충전: +, 사용: -)
         */
        public long getPointChange() {
                return isCharge() ? amount : -amount;
        }
}
