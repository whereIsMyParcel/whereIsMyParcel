package com.sparta.whereismyparcel.hub.presentation.controller;

import com.sparta.whereismyparcel.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.whereismyparcel.hub.application.service.HubCommandService;
import com.sparta.whereismyparcel.hub.application.service.HubQueryService;
import com.sparta.whereismyparcel.hub.presentation.dto.request.CreateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.request.UpdateHubRequest;
import com.sparta.whereismyparcel.hub.presentation.dto.response.HubResponse;
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

@WebMvcTest(HubController.class)
@Import(GlobalExceptionHandler.class)
class HubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HubCommandService hubCommandService;

    @MockBean
    private HubQueryService hubQueryService;

    @Test
    @DisplayName("허브 생성 성공 - MASTER 권한")
    void createHub_Success() throws Exception {
        CreateHubRequest request = new CreateHubRequest("테스트 허브", "서울시", 37.0, 127.0);
        HubResponse response = new HubResponse(UUID.randomUUID(), "테스트 허브", "서울시", 37.0, 127.0);

        given(hubCommandService.createHub(any(), any(), any(), any())).willReturn(response);

        mockMvc.perform(post("/api/v1/hubs")
                        .header("X-User-Role", "MASTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("테스트 허브"));
    }

    @Test
    @DisplayName("허브 생성 실패 - 권한 없음")
    void createHub_Forbidden() throws Exception {
        CreateHubRequest request = new CreateHubRequest("테스트 허브", "서울시", 37.0, 127.0);

        mockMvc.perform(post("/api/v1/hubs")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("허브 단건 조회 성공")
    void getHub_Success() throws Exception {
        UUID hubId = UUID.randomUUID();
        HubResponse response = new HubResponse(hubId, "테스트 허브", "서울시", 37.0, 127.0);

        given(hubQueryService.getHub(hubId)).willReturn(response);

        mockMvc.perform(get("/api/v1/hubs/{hubId}", hubId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("테스트 허브"));
    }

    @Test
    @DisplayName("허브 목록 조회 성공")
    void getHubs_Success() throws Exception {
        Page<HubResponse> page = new PageImpl<>(List.of(
                new HubResponse(UUID.randomUUID(), "허브1", "주소1", 37.0, 127.0)
        ));

        given(hubQueryService.getHubs(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/v1/hubs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("허브1"));
    }

    @Test
    @DisplayName("허브 수정 성공")
    void updateHub_Success() throws Exception {
        UUID hubId = UUID.randomUUID();
        UpdateHubRequest request = new UpdateHubRequest("수정 허브", "수정 주소", 38.0, 128.0);
        HubResponse response = new HubResponse(hubId, "수정 허브", "수정 주소", 38.0, 128.0);

        given(hubCommandService.updateHub(eq(hubId), any(), any(), any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/hubs/{hubId}", hubId)
                        .header("X-User-Role", "HUB_MANAGER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정 허브"));
    }

    @Test
    @DisplayName("허브 삭제 성공")
    void deleteHub_Success() throws Exception {
        UUID hubId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/hubs/{hubId}", hubId)
                        .header("X-User-Role", "MASTER")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk());

        verify(hubCommandService).deleteHub(hubId, "user123");
    }
}
