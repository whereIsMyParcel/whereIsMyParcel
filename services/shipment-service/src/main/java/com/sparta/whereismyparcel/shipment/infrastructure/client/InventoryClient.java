//package com.sparta.whereismyparcel.shipment.infrastructure.client;
//
//import com.sparta.whereismyparcel.common.response.ApiResponse;
//import com.sparta.whereismyparcel.shipment.presentation.dto.request.DecreaseInventoryRequest;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.PatchMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//
//@FeignClient(name = "company-service", path = "/internal/v1/inventories")
//public interface InventoryClient {
//
//    @PatchMapping("/decrease")
//    ApiResponse<Void> decrease(@RequestBody DecreaseInventoryRequest request);
//}
