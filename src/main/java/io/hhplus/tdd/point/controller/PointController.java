package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final PointService pointService;
    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable("id") long userId
    ) {

        UserPoint userPoint = pointService.getUserPoint(userId);
        return userPoint;
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable("id") long userId
    ) {
        List<PointHistory> pointHistory = pointService.getPointHistory(userId);
        log.info("pointHistory: {}", pointHistory);
        log.info("pointHistory size: {}", pointHistory.size());
        log.info("pointHistory[0]: {}", pointHistory.get(0));
        log.info("pointHistory[0].id: {}", pointHistory.get(0).id());
        log.info("pointHistory[0].userId: {}", pointHistory.get(0).userId());
        log.info("pointHistory[0].amount: {}", pointHistory.get(0).amount());
        log.info("pointHistory[0].transactionType: {}", pointHistory.get(0).type());
        log.info("pointHistory[0].createdAt: {}", pointHistory.get(0).updateMillis());

        return pointHistory;
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        UserPoint userPoint = pointService.chargePoint(id, amount);
        return userPoint;
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        UserPoint userPoint = pointService.usePoint(id, amount);
        return userPoint;
    }
}
