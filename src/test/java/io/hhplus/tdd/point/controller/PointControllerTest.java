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

/**
 * PointController 웹 계층 테스트
 * 
 * 테스트 목적:
 * 1. HTTP 요청/응답 처리의 정확성 검증
 * 2. API 계약(Contract) 준수 확인
 * 3. JSON 직렬화/역직렬화 검증
 * 4. HTTP 상태 코드 적절성 확인
 * 5. 컨트롤러와 서비스 계층 간의 협력 검증
 * 
 * 주의사항:
 * - @WebMvcTest로 웹 계층만 격리하여 테스트
 * - 서비스 로직은 Mock으로 대체하여 컨트롤러 로직에 집중
 * - 실제 비즈니스 로직 테스트는 서비스 테스트에서 담당
 */
@WebMvcTest(PointController.class)
class PointControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private PointService pointService;

        /**
         * 사용자 포인트 조회 API 테스트
         * 
         * 테스트 이유:
         * - 가장 기본적인 조회 API의 정상 동작 검증
         * - HTTP GET 요청 처리 및 JSON 응답 형식 확인
         * - Path Variable 바인딩의 정확성 검증
         * - 서비스 계층과의 올바른 데이터 전달 확인
         * - API 응답 필드명과 타입의 일관성 보장
         */
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

        /**
         * 포인트 충전 API 테스트
         * 
         * 테스트 이유:
         * - HTTP PATCH 요청 처리 및 요청 본문 파싱 검증
         * - 충전 비즈니스 로직 호출의 정확성 확인
         * - 충전 후 응답 데이터의 정확성 검증
         * - Content-Type 헤더 처리 확인 (application/json)
         * - RESTful API 설계 원칙 준수 검증
         */
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

        /**
         * 포인트 사용 API 테스트
         * 
         * 테스트 이유:
         * - 사용 API의 정상적인 HTTP 처리 검증
         * - 사용 후 잔고 반영의 정확성 확인
         * - 충전과 사용 API의 일관된 응답 형식 보장
         * - 서비스 계층 메서드 호출 검증
         */
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

        /**
         * 포인트 거래 내역 조회 API 테스트
         * 
         * 테스트 이유:
         * - 배열 형태의 JSON 응답 처리 검증
         * - 복수 데이터의 직렬화 정확성 확인
         * - 거래 내역의 필수 필드 포함 여부 검증
         * - List<PointHistory> 타입의 HTTP 응답 변환 확인
         * - 거래 유형(CHARGE/USE) 구분의 정확성 검증
         */
        @Test
        @DisplayName("특정 유저의 포인트 내역 조회 - 성공")
        void getPointHistory_Success() throws Exception {
                // given
                long userId = 1L;
                List<PointHistory> histories = List.of(
                                new PointHistory(1L, userId, 1000L, TransactionType.CHARGE, System.currentTimeMillis()),
                                new PointHistory(2L, userId, 300L, TransactionType.USE, System.currentTimeMillis()));
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

        /**
         * 잘못된 포인트 충전 금액 예외 처리 테스트
         * 
         * 테스트 이유:
         * - 서비스에서 발생한 예외의 HTTP 변환 검증
         * - @ControllerAdvice를 통한 전역 예외 처리 확인
         * - 클라이언트에게 적절한 에러 응답 제공 검증
         * - HTTP 400 Bad Request 상태 코드 반환 확인
         * - API 오용 방지 및 사용성 개선
         */
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

        /**
         * 잘못된 포인트 사용 금액 예외 처리 테스트
         * 
         * 테스트 이유:
         * - 사용 API에서의 예외 처리 일관성 확인
         * - 충전과 사용 모두 동일한 예외 처리 방식 적용 검증
         * - 입력 검증 에러에 대한 적절한 HTTP 응답 확인
         */
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

        /**
         * 잔고 부족으로 인한 포인트 사용 실패 테스트
         * 
         * 테스트 이유:
         * - 비즈니스 규칙 위반 시 적절한 HTTP 응답 검증
         * - 클라이언트가 잔고 부족 상황을 인지할 수 있도록 함
         * - 금융 시스템의 핵심 제약 조건 처리 확인
         * - 서비스 계층의 비즈니스 예외를 HTTP 에러로 적절히 변환하는지 검증
         */
        @Test
        @DisplayName("잔고 부족으로 포인트 사용 실패")
        void usePoint_InsufficientBalance_Fail() throws Exception {
                // given
                long userId = 1L;
                long amount = 10000L; // ✅ 실제 값 사용
                given(pointService.usePoint(userId, amount)) // ✅ 모두 실제 값
                                .willThrow(new IllegalArgumentException("잔고가 부족합니다"));

                // when & then
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(amount)))
                                .andExpect(status().isBadRequest());
        }
}