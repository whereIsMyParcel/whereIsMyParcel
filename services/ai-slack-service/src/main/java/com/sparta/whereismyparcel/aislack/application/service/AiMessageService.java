package com.sparta.whereismyparcel.aislack.application.service;

import com.sparta.whereismyparcel.aislack.domain.entity.AiMessage;
import com.sparta.whereismyparcel.aislack.domain.entity.AnalysisStatus;
import com.sparta.whereismyparcel.aislack.domain.exception.AiSlackErrorCode;
import com.sparta.whereismyparcel.aislack.domain.repository.AiMessageRepository;
import com.sparta.whereismyparcel.aislack.infrastructure.client.GeminiClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.OrderFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.ShipmentFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.UserFeignClient;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.DeliveryDeadlinePatchRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.GeminiRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.request.OrderInternalRequest;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.GeminiResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.OrderResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.ShipmentResponse;
import com.sparta.whereismyparcel.aislack.infrastructure.client.dto.response.UserResponse;
import com.sparta.whereismyparcel.common.exception.BusinessException;
import com.sparta.whereismyparcel.common.response.ApiResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 클래스 레벨의 readOnly 트랜잭션은 유지
public class AiMessageService {

    private final AiMessageRepository aiMessageRepository;
    private final OrderFeignClient orderFeignClient;
    private final ShipmentFeignClient shipmentFeignClient;
    private final UserFeignClient userFeignClient;
    private final GeminiClient geminiClient;
    private final PromptGenerator promptGenerator;

    // Self-injection for transactional method calls
    private final ApplicationContext applicationContext;
    private AiMessageService self;

    @PostConstruct
    public void init() {
        self = applicationContext.getBean(AiMessageService.class);
    }

    /**
     * AI 분석 요청을 생성하고 초기 상태로 저장합니다.
     * 이 메서드는 주문/배송 정보 생성 시 호출되어 AI 분석을 위한 초기 데이터를 준비합니다.
     * @param request 분석할 주문 ID
     * @param callerUserId 요청을 시작한 사용자 ID (Feign Client 호출 시 필요)
     * @return 생성된 AiMessage의 ID
     */
    @Transactional // 이 메서드는 DB 쓰기 작업이므로 트랜잭션 유지
    public UUID createAiAnalysisRequest(OrderInternalRequest request, String callerUserId) {
        UUID orderId = request.orderId();

        // 1. 주문, 배송, 사용자 정보 가져오기
        OrderResponse order = getOrderDetails(orderId, callerUserId);
        List<ShipmentResponse> shipments = getShipmentDetails(orderId, callerUserId);
        UserResponse recipientUser = getUserDetails(order.recipientName(), callerUserId);

        // 2. Gemini AI 프롬프트 생성
        String prompt = promptGenerator.createGeminiPrompt(order, shipments, recipientUser);

        // 3. AiMessage 엔티티 생성 및 REQUESTED 상태로 저장
        AiMessage aiMessage = AiMessage.create(orderId, prompt);
        aiMessageRepository.save(aiMessage);

        log.info("AI 분석 요청 생성: orderId={}, aiMessageId={}", orderId, aiMessage.getAiId());
        return aiMessage.getAiId();
    }

    /**
     * Gemini 분석 완료 후, Order 서비스에 최적 발송 시한을 패치합니다.
     * 이 메서드는 외부 API 호출을 담당하므로 트랜잭션이 필요 없습니다.
     * @param request 주문 ID와 최적 발송 시한 정보를 담은 DTO
     * @param callerUserId 요청을 시작한 사용자 ID
     */
    // @Transactional 제거: 외부 API 호출은 트랜잭션 범위 밖에서 수행
    public void patchDeliveryDeadlineToOrderService(DeliveryDeadlinePatchRequest request, String callerUserId) {
        log.info("Order 서비스에 delivery_deadline 패치 요청: orderId={}, deliveryDeadline={}", request.orderId(), request.deliveryDeadline());
        ApiResponse<Void> response = orderFeignClient.patchDeliveryDeadline(callerUserId, request.orderId(), request);
        if (!response.success()) {
            throw new BusinessException(AiSlackErrorCode.ORDER_SERVICE_COMMUNICATION_FAILED, "Order 서비스에 delivery_deadline 패치 실패: " + response.message());
        }
    }

    /**
     * 특정 AiMessage에 대해 Gemini AI 분석을 수행하고 결과를 업데이트합니다.
     * 이 메서드는 외부 API 호출을 포함하므로 트랜잭션 범위 밖에서 오케스트레이션 역할을 합니다.
     * @param aiMessageId 분석을 수행할 AiMessage의 ID
     */
    // @Transactional 제거: 외부 API 호출을 포함하므로 트랜잭션 범위 밖에서 실행
    public void analyzeAiMessage(UUID aiMessageId) {
        AiMessage aiMessage = null;
        try {
            // 1. AiMessage 조회 및 재시도 상태로 준비 (새로운 트랜잭션)
            aiMessage = self.getAndPrepareAiMessageForAnalysis(aiMessageId);

            // 2. Gemini AI 호출 (외부 API)
            GeminiResponse geminiResponse = callGeminiApi(aiMessage.getRequestContent());
            String aiResponseContent = extractTextFromGeminiResponse(geminiResponse);
            LocalDateTime finalDispatchDeadline = extractFinalDispatchDeadline(aiResponseContent);

            // 3. AiMessage 상태 업데이트 및 결과 저장 (새로운 트랜잭션)
            self.updateAiMessageStatusOnSuccess(aiMessageId, aiResponseContent, finalDispatchDeadline);
            log.info("AI 분석 성공: aiMessageId={}", aiMessageId);

            // 4. Order 서비스에 delivery_deadline 패치 (외부 API)
            DeliveryDeadlinePatchRequest patchRequest = new DeliveryDeadlinePatchRequest(aiMessage.getOrderId(), finalDispatchDeadline);
            patchDeliveryDeadlineToOrderService(patchRequest, "ai-slack-internal-service");
            log.info("Order 서비스에 최종 발송 시한 패치 완료: orderId={}, deliveryDeadline={}", aiMessage.getOrderId(), finalDispatchDeadline);

        } catch (BusinessException e) {
            log.error("AI 분석 실패: aiMessageId={}, error={}", aiMessageId, e.getMessage());
            if (aiMessage != null) {
                self.updateAiMessageStatusOnFailure(aiMessageId); // 실패 상태 업데이트 (새로운 트랜잭션)
            }
            throw e; // 실패 예외를 다시 던져 상위에서 처리하도록 함
        } catch (Exception e) {
            log.error("예상치 못한 AI 분석 오류: aiMessageId={}, error={}", aiMessageId, e.getMessage(), e);
            if (aiMessage != null) {
                self.updateAiMessageStatusOnFailure(aiMessageId); // 실패 상태 업데이트 (새로운 트랜잭션)
            }
            throw new BusinessException(AiSlackErrorCode.AI_PROCESSING_FAILED, e.getMessage());
        }
        // finally 블록에서 save를 호출할 필요 없음. 각 상태 업데이트 메서드에서 save를 처리.
    }

    /**
     * AiMessage를 조회하고, 필요한 경우 재시도를 위해 상태를 REQUESTED로 변경합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected AiMessage getAndPrepareAiMessageForAnalysis(UUID aiMessageId) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.INVALID_AI_REQUEST_DATA, "AI 메시지를 찾을 수 없습니다: " + aiMessageId));

        // 이미 처리되었거나 영구 실패한 메시지는 다시 분석하지 않음 (멱등성)
        if (aiMessage.getAnalysisStatus() == AnalysisStatus.AI_SUCCESS) {
            log.info("AiMessage {}는 이미 AI_SUCCESS 상태입니다. 분석 하지않습니다..", aiMessageId);
            // 이미 성공 상태이므로, 이 시점에서 예외를 던지거나 특별한 처리를 하지 않고,
            // analyzeAiMessage 호출자가 이 AiMessage를 가지고 다음 로직을 진행하지 않도록 null 또는 특정 상태를 반환할 수 있으나,
            // 현재 analyzeAiMessage 로직에서는 이 메서드의 반환값을 바로 사용하므로, 여기서는 예외를 던지는 것이 명확합니다.
            // 혹은 analyzeAiMessage에서 이 메서드 호출 전에 상태를 한 번 더 체크하는 것이 좋습니다.
            // 여기서는 analyzeAiMessage에서 이미 체크하고 들어온다고 가정하고, 만약의 경우를 대비해 예외를 던집니다.
            throw new BusinessException(AiSlackErrorCode.AI_PROCESSING_FAILED, "이미 성공한 AI 메시지입니다: " + aiMessageId);
        }

        // AI 분석 재시도 시 상태를 REQUESTED로 변경
        if (aiMessage.getAnalysisStatus() == AnalysisStatus.AI_FAIL) {
            aiMessage.requeueForAnalysis(); // 상태를 REQUESTED로 변경하고 응답 필드 초기화
            aiMessageRepository.save(aiMessage);
            log.info("AiMessage {} 재분석을 위해 REQUESTED 상태로 변경되었습니다.", aiMessageId);
        }
        return aiMessage;
    }

    /**
     * AI 분석 성공 시 AiMessage의 상태를 업데이트합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateAiMessageStatusOnSuccess(UUID aiMessageId, String responseContent, LocalDateTime finalDispatchDeadline) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.INVALID_AI_REQUEST_DATA, "AI 메시지를 찾을 수 없습니다: " + aiMessageId));
        aiMessage.succeedAnalysis(responseContent, finalDispatchDeadline);
        aiMessageRepository.save(aiMessage);
    }

    /**
     * AI 분석 실패 시 AiMessage의 상태를 업데이트합니다.
     * 이 메서드는 새로운 트랜잭션에서 실행됩니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateAiMessageStatusOnFailure(UUID aiMessageId) {
        AiMessage aiMessage = aiMessageRepository.findById(aiMessageId)
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.INVALID_AI_REQUEST_DATA, "AI 메시지를 찾을 수 없습니다: " + aiMessageId));
        aiMessage.failAnalysis();
        aiMessageRepository.save(aiMessage);
    }


    private OrderResponse getOrderDetails(UUID orderId, String UserId) {
        ApiResponse<OrderResponse> orderResponse = orderFeignClient.getOrder(UserId, orderId);
        if (!orderResponse.success() || orderResponse.data() == null) {
            throw new BusinessException(AiSlackErrorCode.INVALID_AI_REQUEST_DATA, "주문 정보를 가져오지 못했습니다: " + orderResponse.message());
        }
        return orderResponse.data();
    }

    private List<ShipmentResponse> getShipmentDetails(UUID orderId, String UserId) {
        ApiResponse<List<ShipmentResponse>> shipmentResponse = shipmentFeignClient.getShipmentByOrderId(UserId, orderId);
        if (!shipmentResponse.success() || shipmentResponse.data() == null || shipmentResponse.data().isEmpty()) {
            throw new BusinessException(AiSlackErrorCode.INVALID_AI_REQUEST_DATA, "배송 정보를 가져오지 못했습니다: " + shipmentResponse.message());
        }
        return shipmentResponse.data();
    }

    private UserResponse getUserDetails(String recipientName, String UserId) {
        // 실제로는 recipientName으로 사용자 ID를 조회하는 API가 필요할 수 있습니다.
        // 현재는 임시로 callerUserId를 사용하여 UserResponse를 가져오는 것으로 가정합니다.
        // UserFeignClient는 userId로만 조회 가능하므로, 이 부분은 실제 API에 맞춰 수정이 필요합니다.
        ApiResponse<UserResponse> userResponse = userFeignClient.getUser(UserId);
        if (!userResponse.success() || userResponse.data() == null) {
            throw new BusinessException(AiSlackErrorCode.SLACK_ID_NOT_FOUND, "수령인 사용자 정보를 가져오지 못했습니다: " + userResponse.message());
        }
        return userResponse.data();
    }

    private GeminiResponse callGeminiApi(String prompt) {
        GeminiRequest.Part part = new GeminiRequest.Part(prompt);
        GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
        GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                0.7, 1, 1, 2048, List.of()
        );
        GeminiRequest request = new GeminiRequest(List.of(content), config);

        return geminiClient.generateText(request);
    }

    private String extractTextFromGeminiResponse(GeminiResponse geminiResponse) {
        return geminiResponse.getResponseText()
                .orElseThrow(() -> new BusinessException(AiSlackErrorCode.AI_RESPONSE_PARSING_FAILED, "Gemini AI 응답에서 텍스트를 추출할 수 없습니다."));
    }

    private LocalDateTime extractFinalDispatchDeadline(String aiResponseContent) {
        Pattern pattern = Pattern.compile("DEADLINE:(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(aiResponseContent);

        if (matcher.find()) {
            String deadlineString = matcher.group(1);
            try {
                return LocalDateTime.parse(deadlineString);
            } catch (DateTimeParseException e) {
                log.error("Gemini AI 응답에서 최종 발송 시한 파싱 실패: {}", deadlineString, e);
                throw new BusinessException(AiSlackErrorCode.AI_RESPONSE_PARSING_FAILED, "Gemini AI 응답에서 최종 발송 시한을 파싱할 수 없습니다.");
            }
        }
        throw new BusinessException(AiSlackErrorCode.AI_RESPONSE_PARSING_FAILED, "Gemini AI 응답에서 최종 발송 시한을 찾을 수 없습니다.");
    }
}