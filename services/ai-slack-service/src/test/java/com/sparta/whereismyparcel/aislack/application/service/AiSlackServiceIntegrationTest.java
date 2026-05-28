package com.sparta.whereismyparcel.aislack.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.whereismyparcel.aislack.infrastructure.client.OrderFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.UserFeignClient; // UserFeignClient 임포트 추가
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.OrderInternalRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse; // UserResponse 임포트 추가
import com.sparta.whereismyparcel.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Spring Security 필터 비활성화
class AiSlackServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderFeignClient orderFeignClient;

    @MockitoBean
    private ShipmentFeignClient shipmentFeignClient;

    @MockitoBean // UserFeignClient 모킹 추가
    private UserFeignClient userFeignClient;

    @DisplayName("주문 정보에 LocalDateTime 필드가 포함되어 있을 때 AI 분석 요청이 성공해야 한다")
    @Test
    void createAiAnalysisRequest_withLocalDateTime_shouldSucceed() throws Exception {
        // Given
        UUID orderId = UUID.randomUUID();
        String userId = UUID.randomUUID().toString();

        OrderInternalRequest request = new OrderInternalRequest(orderId);

        // OrderFeignClient가 LocalDateTime 필드를 포함하는 OrderResponse를 반환하도록 모킹
        OrderResponse mockOrderResponse = new OrderResponse(
                orderId,
                "ORDER-12345",
                "테스트 수령인",
                "테스트 주소",
                LocalDateTime.of(2023, 1, 1, 10, 0, 0), // requestedDeliveryAt
                "DELIVERING",
                "문 앞에 놓아주세요",
                LocalDateTime.of(2023, 1, 1, 9, 0, 0), // orderedAt
                Collections.emptyList()
        );
        when(orderFeignClient.getOrder(any(String.class), any(UUID.class)))
                .thenReturn(ApiResponse.success(mockOrderResponse));

        // ShipmentFeignClient가 유효한 ShipmentResponse 리스트를 반환하도록 모킹
        ShipmentResponse mockShipmentResponse = new ShipmentResponse(
                UUID.randomUUID(), // id
                orderId, // orderId
                UUID.randomUUID(), // originHubId
                UUID.randomUUID(), // currentHubId
                UUID.randomUUID(), // destinationHubId
                UUID.randomUUID(), // companyDeliveryManagerId
                "SHIP-001", // shipmentNumber
                "PREPARING", // shipmentStatus
                "테스트 배송 주소", // deliveryAddress
                "테스트 수령인", // recipientName
                "U1234567890", // recipientSlackId
                LocalDateTime.of(2023, 1, 3, 10, 0, 0), // estimatedDeliveryAt
                LocalDateTime.of(2023, 1, 1, 12, 0, 0), // shippedAt
                null // deliveredAt (아직 배송되지 않음)
        );
        when(shipmentFeignClient.getShipmentByOrderId(any(String.class), any(UUID.class)))
                .thenReturn(ApiResponse.success(List.of(mockShipmentResponse))); // 빈 리스트가 아닌 유효한 리스트 반환

        // UserFeignClient가 유효한 UserResponse를 반환하도록 모킹
        UserResponse mockUserResponse = new UserResponse(
                userId,
                "testuser",
                "테스트 사용자",
                "test@example.com",
                "U1234567890"
        );
        when(userFeignClient.getUser(any(UUID.class)))
                .thenReturn(ApiResponse.success(mockUserResponse));

        // When & Then
        mockMvc.perform(post("/internal/v1/ai-slack/analysis-requests")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify that OrderFeignClient.getOrder was called
        verify(orderFeignClient, times(1)).getOrder(userId, orderId);

        // Verify that ShipmentFeignClient.getShipmentByOrderId was called
        verify(shipmentFeignClient, times(1)).getShipmentByOrderId(userId, orderId);

        // Verify that UserFeignClient.getUser was called
        verify(userFeignClient, times(1)).getUser(UUID.fromString(userId));
    }
}
