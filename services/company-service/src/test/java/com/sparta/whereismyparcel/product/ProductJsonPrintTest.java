package com.sparta.whereismyparcel.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sparta.whereismyparcel.product.presentation.dto.request.*;

import com.sparta.whereismyparcel.product.application.service.ProductService;
import com.sparta.whereismyparcel.product.presentation.dto.request.ProductRegisterRequest;
import com.sparta.whereismyparcel.product.presentation.dto.response.ProductResponse;
import com.sparta.whereismyparcel.product.presentation.dto.response.VariantResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@Transactional // 테스트가 끝나면 DB 데이터를 깔끔하게 롤백시킵니다.
class ProductJsonPrintTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper; // 🔥 JSON 변환을 담당하는 핵심 부품

    @Test
    @DisplayName("상품 등록 시 최종 반환되는 ProductResponse JSON 형태 콘솔 출력하기")
    void printProductResponseJson() throws Exception {
        // 1. 포스트맨으로 보낼 가짜 요청 데이터(Request DTO) 조립
        ProductRegisterRequest request = new ProductRegisterRequest(
                "나이키 에어포스 '07",
                UUID.randomUUID(), // companyId
                UUID.randomUUID(), // hubId
                "클래식한 올백 에어포스",
                139000,
                List.of(
                        new OptionRegisterRequest("색상", List.of(
                                new OptionValueRegisterRequest("블랙", 0),
                                new OptionValueRegisterRequest("화이트", 0)
                        )),
                        new OptionRegisterRequest("사이즈", List.of(
                                new OptionValueRegisterRequest("260", 0),
                                new OptionValueRegisterRequest("270", 5000) // 추가 금액 테스트
                        ))
                )
        );

        // 2. 승민님이 작성하신 대대장 로직 실행 (DB 저장 및 데카르트 곱 수행)
        ProductResponse response = productService.registerProduct(request);

        // 3. ✨ 핵심: ObjectMapper 설정을 정렬하여 포스트맨처럼 이쁘게(Pretty Print) 출력하기
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonResult = objectMapper.writeValueAsString(response);

        // 4. 인텔리제이 콘솔창에 출력해서 눈으로 확인!
        System.out.println("=================================================");
        System.out.println("🚀 [POSTMAN 미리보기] 최종 반환된 ProductResponse JSON 구조");
        System.out.println("=================================================");
        System.out.println(jsonResult);
        System.out.println("=================================================");
    }

    @Test
    @DisplayName("상품 단건 조회 시 최종 반환되는 ProductResponse JSON 형태 콘솔 출력하기")
    void printGetProductResponseJson() throws Exception {
        // 1. 테스트용 데이터를 먼저 DB에 저장 (영속화)
        ProductRegisterRequest registerRequest = new ProductRegisterRequest(
                "나이키 에어포스 '07",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "클래식한 올백 에어포스",
                139000,
                List.of(
                        new OptionRegisterRequest("색상", List.of(
                                new OptionValueRegisterRequest("블랙", 0),
                                new OptionValueRegisterRequest("화이트", 0)
                        )),
                        new OptionRegisterRequest("사이즈", List.of(
                                new OptionValueRegisterRequest("260", 0),
                                new OptionValueRegisterRequest("270", 5000)
                        ))
                )
        );

        // 우선 등록 로직을 실행해서 실제 DB(영속성 컨텍스트)에 데이터를 집어넣고 ID를 받아옵니다.
        ProductResponse registeredProduct = productService.registerProduct(registerRequest);
        UUID savedProductId = registeredProduct.productId();

        // 2. 💡 승민님이 작성하신 단건 조회 메서드(getProduct) 호출!
        // 실제 컨트롤러가 동작하는 것처럼 findById와 스트림 조립 로직이 가동됩니다.
        ProductResponse response = productService.getProduct(savedProductId);

        // 3. 포스트曼 미리보기처럼 이쁘게 정렬(Indent)하여 문자열로 파싱
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonResult = objectMapper.writeValueAsString(response);

        // 4. 인텔리제이 콘솔창에 출력
        System.out.println("=================================================");
        System.out.println("🔍 [GET - POSTMAN 미리보기] 상품 단건 조회 결과 JSON");
        System.out.println("=================================================");
        System.out.println(jsonResult);
        System.out.println("=================================================");
    }


    @Test
    @DisplayName("상품의 베리언트 목록만 조회 시 반환되는 JSON 배열 형태 콘솔 출력하기")
    void printGetProductVariantsJson() throws Exception {
        // 1. 테스트용 데이터를 먼저 DB에 저장 (영속화)
        ProductRegisterRequest registerRequest = new ProductRegisterRequest(
                "나이키 에어포스 '07",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "클래식한 올백 에어포스",
                139000,
                List.of(
                        new OptionRegisterRequest("색상", List.of(
                                new OptionValueRegisterRequest("블랙", 0),
                                new OptionValueRegisterRequest("화이트", 0)
                        )),
                        new OptionRegisterRequest("사이즈", List.of(
                                new OptionValueRegisterRequest("260", 0),
                                new OptionValueRegisterRequest("270", 5000)
                        ))
                )
        );

        // 우선 등록 로직을 실행해서 DB에 집어넣고 생성된 상품 ID를 받아옵니다.
        ProductResponse registeredProduct = productService.registerProduct(registerRequest);
        UUID savedProductId = registeredProduct.productId();

        // 2. ✨ 방금 수정한 리스트 반환 메서드(getProductVariants) 호출!
        List<VariantResponse> response = productService.getVariants(savedProductId);

        // 3. 포스트맨 미리보기처럼 이쁘게 줄바꿈(Indent) 정렬 설정 후 JSON 변환
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonResult = objectMapper.writeValueAsString(response);

        // 4. 인텔리제이 콘솔창에 출력
        System.out.println("=================================================");
        System.out.println("📦 [GET - POSTMAN 미리보기] 오직 베리언트 리스트만 출력");
        System.out.println("=================================================");
        System.out.println(jsonResult);
        System.out.println("=================================================");
    }
}