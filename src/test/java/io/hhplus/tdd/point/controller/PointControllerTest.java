package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;


    @Test
    @DisplayName("특정 유저의 포인트 조회 - 성공")
    void getUserPoint_Success() throws Exception {
        // given
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
        given(pointService.getUserPoint(userId)).willReturn(userPoint);

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1000L));

        verify(pointService).getUserPoint(userId);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전 - 성공")
    void chargePoint_Success() throws Exception {
        // given
        long userId = 1L;
        long amount = 500L;
        UserPoint userPoint = new UserPoint(userId, 1500L, System.currentTimeMillis());
        given(pointService.chargePoint(userId, amount)).willReturn(userPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1500L));

        verify(pointService).chargePoint(userId, amount);
    }

    @Test
    @DisplayName("특정 유저의 포인트 사용 - 성공")
    void usePoint_Success() throws Exception {
        // given
        long userId = 1L;
        long amount = 300L;
        UserPoint userPoint = new UserPoint(userId, 700L, System.currentTimeMillis());
        given(pointService.usePoint(userId, amount)).willReturn(userPoint);

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(700L));

        verify(pointService).usePoint(userId, amount);
    }

    @Test
    @DisplayName("특정 유저의 포인트 내역 조회 - 성공")
    void getPointHistory_Success() throws Exception {
        // given
        long userId = 1L;
        List<PointHistory> histories = List.of(
                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 300L, TransactionType.USE, System.currentTimeMillis())
        );
        given(pointService.getPointHistory(userId)).willReturn(histories);

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(1000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].userId").value(userId))
                .andExpect(jsonPath("$[1].amount").value(300L))
                .andExpect(jsonPath("$[1].type").value("USE"));

        verify(pointService).getPointHistory(userId);
    }

    @Test
    @DisplayName("잘못된 포인트 충전 금액 - 실패")
    void chargePoint_WithNegativeAmount_Fail() throws Exception {
        // given
        long userId = 1L;
        long amount = -100L;
        given(pointService.chargePoint(userId, amount))
                .willThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다"));

        // when & then
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 포인트 사용 금액 - 실패")
    void usePoint_WithNegativeAmount_Fail() throws Exception {
        // given
        long userId = 1L;
        long amount = -100L;
        given(pointService.usePoint(userId, amount))
                .willThrow(new IllegalArgumentException("사용 금액은 0보다 커야 합니다"));

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잔고 부족으로 포인트 사용 실패")
    void usePoint_InsufficientBalance_Fail() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        given(pointService.usePoint(userId, amount))
                .willThrow(new IllegalArgumentException("잔고가 부족합니다"));

        // when & then
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(amount)))
                .andExpect(status().isBadRequest());
    }
} 