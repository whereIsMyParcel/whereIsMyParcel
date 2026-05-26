package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.exception.GlobalExceptionHandler;
import com.sparta.whereismyparcel.hub.application.service.HubQueryService;
import com.sparta.whereismyparcel.hub.application.service.ShortestPathService;
import com.sparta.whereismyparcel.hub.domain.exception.HubNotFoundException;
import com.sparta.whereismyparcel.hub.presentation.dto.response.ShortestPathResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@WebMvcTest(HubInternalController.class)
@Import(GlobalExceptionHandler.class)
class HubInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HubQueryService hubQueryService;

    @MockBean
    private ShortestPathService shortestPathService;

    @Test
    @DisplayName("허브 존재 여부 - 존재함")
    void checkHubExists_True() throws Exception {
        UUID hubId = UUID.randomUUID();
        given(hubQueryService.getHub(hubId)).willReturn(null);

        mockMvc.perform(get("/internal/v1/hubs/{hubId}/exists", hubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    @DisplayName("허브 존재 여부 - 존재하지 않음")
    void checkHubExists_False() throws Exception {
        UUID hubId = UUID.randomUUID();
        given(hubQueryService.getHub(hubId)).willThrow(new HubNotFoundException());

        mockMvc.perform(get("/internal/v1/hubs/{hubId}/exists", hubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("최단 경로 조회 성공")
    void getShortestPath_Success() throws Exception {
        UUID originId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        ShortestPathResponse response = new ShortestPathResponse(1000.0, 60, List.of(
                new ShortestPathResponse.RouteSegmentResponse(1, originId, "출발", destId, "도착", 1000.0, 60)
        ));

        given(shortestPathService.getShortestPath(originId, destId)).willReturn(response);

        mockMvc.perform(get("/internal/v1/hub-routes/shortest-path")
                        .param("originHubId", originId.toString())
                        .param("destinationHubId", destId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDistance").value(1000.0));
    }
}
