package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DecreaseInventoryRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.GetDestinationHubIdRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetDestinationHubIdResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetProductHubIdResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "company-service", fallbackFactory = CompanyClientFallbackFactory.class)
public interface CompanyClient {

    @PostMapping("/internal/v1/companies/search-hub")
    ApiResponse<GetDestinationHubIdResponse> getDestinationHubId(@RequestBody GetDestinationHubIdRequest request);

    @PostMapping("/internal/v1/inventories/confirm")
    ApiResponse<Void> decrease(@RequestBody DecreaseInventoryRequest request);

    @GetMapping("/internal/v1/products/search-hub")
    ApiResponse<List<GetProductHubIdResponse>> getHubMappingsByProductIds(
            @RequestParam("productVariantId") List<UUID> productVariantIds
    );
}
