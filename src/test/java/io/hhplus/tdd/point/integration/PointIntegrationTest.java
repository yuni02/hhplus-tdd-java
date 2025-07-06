package io.hhplus.tdd.point.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.tdd.point.domain.PointHistory;
import io.hhplus.tdd.point.domain.UserPoint;
import io.hhplus.tdd.point.domain.constant.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("포인트 충전 및 사용 통합 테스트")
    void pointChargeAndUseIntegrationTest() throws Exception {
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 300L;

        // 1. 초기 포인트 조회
        MvcResult initialResult = mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andReturn();

        UserPoint initialPoint = objectMapper.readValue(
                initialResult.getResponse().getContentAsString(), UserPoint.class);
        long initialBalance = initialPoint.point();

        // 2. 포인트 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(chargeAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(initialBalance + chargeAmount));

        // 3. 포인트 사용
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(initialBalance + chargeAmount - useAmount));

        // 4. 포인트 내역 조회
        MvcResult historyResult = mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        PointHistory[] histories = objectMapper.readValue(
                historyResult.getResponse().getContentAsString(), PointHistory[].class);
        
        assertThat(histories).hasSizeGreaterThanOrEqualTo(2);
        
        // 충전 내역 확인
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
        
        assertThat(hasChargeHistory).isTrue();
        assertThat(hasUseHistory).isTrue();
    }

    @Test
    @DisplayName("잔고 부족으로 포인트 사용 실패 통합 테스트")
    void insufficientBalanceIntegrationTest() throws Exception {
        long userId = 999L; // 새로운 사용자 ID
        long useAmount = 10000L; // 큰 금액

        // 잔고 부족으로 포인트 사용 실패
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isBadRequest());
    }
} 