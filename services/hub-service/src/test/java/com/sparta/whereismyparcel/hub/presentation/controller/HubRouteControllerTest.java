package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.whereismyparcel.hub.application.service.HubRouteCommandService;
import com.sparta.whereismyparcel.hub.application.service.HubRouteQueryService;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRouteRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubRouteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@WebMvcTest(HubRouteController.class)
@Import(GlobalExceptionHandler.class)
class HubRouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HubRouteCommandService hubRouteCommandService;

    @MockBean
    private HubRouteQueryService hubRouteQueryService;

    @Test
    @DisplayName("허브 경로 생성 성공")
    void createHubRoute_Success() throws Exception {
        CreateHubRouteRequest request = new CreateHubRouteRequest(UUID.randomUUID(), UUID.randomUUID(), 1000.0, 60);
        HubRouteResponse response = new HubRouteResponse(UUID.randomUUID(), request.originHubId(), "출발", request.destinationHubId(), "도착", 1000.0, 60);

        given(hubRouteCommandService.createHubRoute(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/hub-routes")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.distance").value(1000.0));
    }

    @Test
    @DisplayName("허브 경로 단건 조회 성공")
    void getHubRoute_Success() throws Exception {
        UUID routeId = UUID.randomUUID();
        HubRouteResponse response = new HubRouteResponse(routeId, UUID.randomUUID(), "출발", UUID.randomUUID(), "도착", 1000.0, 60);

        given(hubRouteQueryService.getHubRoute(routeId)).willReturn(response);

        mockMvc.perform(get("/api/v1/hub-routes/{routeId}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distance").value(1000.0));
    }

    @Test
    @DisplayName("허브 경로 목록 조회 성공")
    void getHubRoutes_Success() throws Exception {
        Page<HubRouteResponse> page = new PageImpl<>(List.of(
                new HubRouteResponse(UUID.randomUUID(), UUID.randomUUID(), "출발", UUID.randomUUID(), "도착", 1000.0, 60)
        ));

        given(hubRouteQueryService.getHubRoutes(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/hub-routes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].distance").value(1000.0));
    }

    @Test
    @DisplayName("허브 경로 수정 성공")
    void updateHubRoute_Success() throws Exception {
        UUID routeId = UUID.randomUUID();
        UpdateHubRouteRequest request = new UpdateHubRouteRequest(2000.0, 120);
        HubRouteResponse response = new HubRouteResponse(routeId, UUID.randomUUID(), "출발", UUID.randomUUID(), "도착", 2000.0, 120);

        given(hubRouteCommandService.updateHubRoute(eq(routeId), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/hub-routes/{routeId}", routeId)
                        .header("X-User-Role", "HUB_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.distance").value(2000.0));
    }

    @Test
    @DisplayName("허브 경로 삭제 성공")
    void deleteHubRoute_Success() throws Exception {
        UUID routeId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/hub-routes/{routeId}", routeId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(hubRouteCommandService).deleteHubRoute(routeId, "user123");
    }
}
