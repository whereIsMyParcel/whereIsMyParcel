package com.sparta.whereismyparcel.inventory.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.inventory.application.service.InventoryService;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.AddInventoryRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.AddInventoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<AddInventoryResponse>> addStock(
            @RequestBody @Valid AddInventoryRequest request) {
        AddInventoryResponse response = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

}
