package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.DecreaseInventoryRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.GetDestinationHubIdRequest;
import com.sparta.whereismyparcel.shipment.presentation.dto.response.GetProductHubIdResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyClient {

    @PostMapping("/internal/v1/companies")
    ApiResponse<UUID> getDestinationHubId(@RequestBody GetDestinationHubIdRequest request);

    @PatchMapping("/internal/v1/inventories/decrease")
    ApiResponse<Void> decrease(@RequestBody DecreaseInventoryRequest request);

    @PostMapping("/internal/v1/products")
    ApiResponse<List<GetProductHubIdResponse>> getHubMappingsByProductIds(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Set<UUID> productVariantIds
    );
}
