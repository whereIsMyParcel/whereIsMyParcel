package com.sparta.whereismyparcel.shipment.infrastructure.client;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.shipment.presentation.dto.request.GetDestinationHubIdRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "company-service", path = "/internal/v1/companies")
public interface CompanyClient {

    @PostMapping()
    ApiResponse<UUID> getDestinationHubId(@RequestBody GetDestinationHubIdRequest request);
}
