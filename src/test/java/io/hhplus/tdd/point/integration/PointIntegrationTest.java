package io.hhplus.tdd.point.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // ✅ 올바른 어노테이션
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PointIntegrationTest - 통합 테스트
 * 
 * 테스트 목적:
 * 1. 전체 애플리케이션 레이어 간의 통합 검증
 * 2. HTTP 요청부터 응답까지의 전체 플로우 테스트
 * 3. 실제 비즈니스 시나리오 검증
 * 4. 컨트롤러, 서비스, 레포지토리 간의 협력 확인
 * 
 * 주의사항:
 * - 실제 Spring Boot 애플리케이션 컨텍스트 로드
 * - 모든 Bean과 설정이 실제 환경과 동일하게 동작
 * - 단위 테스트보다 느리지만 전체 시스템 검증 가능
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // ✅ 명시적 웹 환경 설정
@AutoConfigureMockMvc // ✅ MockMvc Bean 자동 생성
@ActiveProfiles("test")
class PointIntegrationTest {

        @Autowired
        private MockMvc mockMvc; // ✅ 이제 정상적으로 주입됨

        @Autowired
        private ObjectMapper objectMapper;

        /**
         * 포인트 충전 및 사용 전체 플로우 통합 테스트
         * 
         * 테스트 시나리오:
         * 1. 초기 포인트 상태 확인
         * 2. 포인트 충전 및 잔고 확인
         * 3. 포인트 사용 및 잔고 확인
         * 4. 거래 내역 정확성 검증
         * 
         * 테스트 이유:
         * - 실제 사용자 시나리오와 동일한 플로우 검증
         * - 레이어 간 데이터 전달의 정확성 확인
         * - HTTP 요청/응답 형식 검증
         * - 비즈니스 로직의 end-to-end 검증
         */
        @Test
        @DisplayName("포인트 충전 및 사용 통합 테스트")
        void pointChargeAndUseIntegrationTest() throws Exception {
                long userId = 1L;
                long chargeAmount = 1000L;
                long useAmount = 300L;

                // 1. 초기 포인트 조회 - 시스템 현재 상태 파악
                MvcResult initialResult = mockMvc.perform(get("/point/{id}", userId))
                                .andExpect(status().isOk())
                                .andReturn();

                UserPoint initialPoint = objectMapper.readValue(
                                initialResult.getResponse().getContentAsString(), UserPoint.class);
                long initialBalance = initialPoint.point();

                // 2. 포인트 충전 - 비즈니스 핵심 기능 테스트
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(chargeAmount)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId))
                                .andExpect(jsonPath("$.point").value(initialBalance + chargeAmount));

                // 3. 포인트 사용 - 잔고 차감 로직 검증
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId))
                                .andExpect(jsonPath("$.point").value(initialBalance + chargeAmount - useAmount));

                // 4. 포인트 내역 조회 - 거래 기록 무결성 검증
                MvcResult historyResult = mockMvc.perform(get("/point/{id}/histories", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andReturn();

                PointHistory[] histories = objectMapper.readValue(
                                historyResult.getResponse().getContentAsString(), PointHistory[].class);

                // 최소 2개의 거래 내역이 있어야 함 (충전 1회, 사용 1회)
                assertThat(histories).hasSizeGreaterThanOrEqualTo(2);

                // 충전/사용 내역 존재 여부 확인
                boolean hasChargeHistory = false;
                boolean hasUseHistory = false;

                for (PointHistory history : histories) {
                        if (history.type() == TransactionType.CHARGE && history.amount() == chargeAmount) {
                                hasChargeHistory = true;
                        }
                        if (history.type() == TransactionType.USE && history.amount() == useAmount) {
                                hasUseHistory = true;
                        }
                }

                // 비즈니스 로직 검증: 모든 거래가 올바르게 기록되었는지 확인
                assertThat(hasChargeHistory).isTrue();
                assertThat(hasUseHistory).isTrue();
        }

        /**
         * 잔고 부족 시나리오 통합 테스트
         * 
         * 테스트 이유:
         * - 예외 상황에서의 시스템 동작 검증
         * - HTTP 에러 응답 형식 확인
         * - 전체 에러 처리 체인 검증
         */
        @Test
        @DisplayName("잔고 부족으로 포인트 사용 실패 통합 테스트")
        void insufficientBalanceIntegrationTest() throws Exception {
                long userId = 999L; // 새로운 사용자 ID (초기 잔고 0원)
                long useAmount = 10000L; // 잔고보다 큰 금액

                // 잔고 부족으로 포인트 사용 실패 - 에러 처리 검증
                mockMvc.perform(patch("/point/{id}/use", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(useAmount)))
                                .andExpect(status().isBadRequest()); // HTTP 400 Bad Request 응답 확인
        }

        /**
         * 잘못된 입력값 시나리오 통합 테스트
         * 
         * 테스트 이유:
         * - 입력 검증 로직의 전체 체인 확인
         * - API 계약 준수 검증
         */
        @Test
        @DisplayName("잘못된 금액으로 포인트 충전 실패 통합 테스트")
        void invalidAmountChargeIntegrationTest() throws Exception {
                long userId = 1L;
                long invalidAmount = -500L; // 음수 금액

                // 음수 금액으로 충전 시도 - 입력 검증 확인
                mockMvc.perform(patch("/point/{id}/charge", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(String.valueOf(invalidAmount)))
                                .andExpect(status().isBadRequest()); // HTTP 400 Bad Request 응답 확인
        }
}