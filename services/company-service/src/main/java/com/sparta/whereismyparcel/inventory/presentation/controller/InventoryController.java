package com.sparta.whereismyparcel.inventory.presentation.controller;

import com.sparta.whereismyparcel.common.response.ApiResponse;
import com.sparta.whereismyparcel.inventory.application.service.InventoryService;
import com.sparta.whereismyparcel.inventory.presentation.dto.request.AddInventoryRequest;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.AddInventoryResponse;
import com.sparta.whereismyparcel.inventory.presentation.dto.response.InventoryCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "재고 등록", description = "COMPANY_MANAGER")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY_MANAGER')")
    public ResponseEntity<ApiResponse<AddInventoryResponse>> addStock(
            @RequestBody @Valid AddInventoryRequest request) {
        AddInventoryResponse response = inventoryService.addStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @Operation(summary = "재고 조회", description = "ALL")
    @GetMapping
    public ResponseEntity<ApiResponse<InventoryCheckResponse>> getProduct(
            @RequestParam UUID productVariantId) {
        InventoryCheckResponse response = inventoryService.checkStock(productVariantId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
    }
}