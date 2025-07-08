package io.hhplus.tdd.point.domain;

public record UserPoint(
        long id,
        long point,
        long updateMillis) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    /**
     * 금액 유효성 검증 - 0보다 커야 함
     */
    public static void validateAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다");
        }
    }

    /**
     * 포인트 잔고가 유효한지 검증 - 음수가 될 수 없음
     */
    public void validateBalance() {
        if (this.point < 0) {
            throw new IllegalArgumentException("포인트 잔고는 음수가 될 수 없습니다");
        }
    }

    /**
     * 해당 금액을 사용할 수 있는지 확인
     */
    public boolean canUse(long amount) {
        validateAmount(amount);
        return this.point >= amount;
    }

    /**
     * 포인트 충전 - 새로운 UserPoint 객체 반환 (불변성 유지)
     */
    public UserPoint charge(long amount) {
        validateAmount(amount);
        long newPoint = this.point + amount;
        UserPoint newUserPoint = new UserPoint(this.id, newPoint, System.currentTimeMillis());
        newUserPoint.validateBalance();
        return newUserPoint;
    }

    /**
     * 포인트 사용 - 새로운 UserPoint 객체 반환 (불변성 유지)
     */
    public UserPoint use(long amount) {
        validateAmount(amount);
        if (!canUse(amount)) {
            throw new IllegalArgumentException("잔고가 부족합니다");
        }
        long newPoint = this.point - amount;
        UserPoint newUserPoint = new UserPoint(this.id, newPoint, System.currentTimeMillis());
        newUserPoint.validateBalance();
        return newUserPoint;
    }

    /**
     * 최대 충전 가능 금액 검증 (비즈니스 룰)
     */
    public boolean canCharge(long amount) {
        validateAmount(amount);
        // 예시: 최대 포인트 한도 체크 (1,000,000원)
        return this.point + amount <= 1_000_000L;
    }

    /**
     * 안전한 포인트 충전 - 한도 검증 포함
     */
    public UserPoint safeCharge(long amount) {
        if (!canCharge(amount)) {
            throw new IllegalArgumentException("포인트 충전 한도를 초과했습니다");
        }
        return charge(amount);
    }
}
