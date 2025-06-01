package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.service.IContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST controller for contract management
 */
@Tag(name = "Contracts", description = "Manage rental contracts for flats")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
public class ContractController {
    
    private final IContractService contractService;
    
    @Operation(
        summary = "Create a new contract",
        description = "Creates a new rental contract for a flat. Requires MANAGER or ADMIN role. " +
                      "Validates contract dates and checks for overlaps with existing contracts."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Contract created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - contract overlap or active contract already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ContractResponse> createContract(
            @Parameter(description = "Contract details", required = true)
            @Valid @RequestBody ContractRequest request) {
        log.info("Creating new contract for flat: {}", request.getFlatId());
        ContractResponse response = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "Get contract by ID",
        description = "Retrieves contract details by contract ID. Accessible by all authenticated users."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{contractId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<ContractResponse> getContract(
            @Parameter(description = "Contract ID", required = true)
            @PathVariable Long contractId) {
        log.info("Fetching contract: {}", contractId);
        ContractResponse response = contractService.getContractById(contractId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get active contract for flat",
        description = "Retrieves the currently active contract for a specified flat."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active contract found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "No active contract found for the flat",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/flat/{flatId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<ContractResponse> getActiveContract(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable Long flatId) {
        log.info("Fetching active contract for flat: {}", flatId);
        ContractResponse response = contractService.getActiveContractByFlatId(flatId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get contract history for flat",
        description = "Retrieves all contracts (including expired and cancelled) for a flat."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract history retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ContractSummaryResponse.class))
            )
        )
    })
    @GetMapping("/flat/{flatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<ContractSummaryResponse>> getContractsByFlat(
            @Parameter(description = "Flat ID", required = true)
            @PathVariable Long flatId) {
        log.info("Fetching contracts for flat: {}", flatId);
        List<ContractSummaryResponse> contracts = contractService.getContractsByFlatId(flatId);
        return ResponseEntity.ok(contracts);
    }
    
    @Operation(
        summary = "Get contracts for building",
        description = "Retrieves all contracts for flats in a building with pagination support."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contracts retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        )
    })
    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Page<ContractSummaryResponse>> getContractsByBuilding(
            @Parameter(description = "Building ID", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.info("Fetching contracts for building: {} (page: {}, size: {})", buildingId, page, size);
        
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ContractSummaryResponse> contracts = contractService.getContractsByBuildingId(buildingId, pageable);
        return ResponseEntity.ok(contracts);
    }
    
    @Operation(
        summary = "Search contracts by tenant name",
        description = "Search contracts by tenant name with pagination support."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search results retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        )
    })
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Page<ContractSummaryResponse>> searchContracts(
            @Parameter(description = "Search term for tenant name", required = true)
            @RequestParam String search,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Searching contracts with term: {}", search);
        Pageable pageable = PageRequest.of(page, size);
        Page<ContractSummaryResponse> results = contractService.searchContractsByTenantName(search, pageable);
        return ResponseEntity.ok(results);
    }
    
    @Operation(
        summary = "Renew a contract",
        description = "Creates a renewal contract extending the current contract. " +
                      "The current contract must be active and expiring soon. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract renewed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - contract cannot be renewed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{contractId}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ContractResponse> renewContract(
            @Parameter(description = "Contract ID to renew", required = true)
            @PathVariable Long contractId,
            @Parameter(description = "Renewal details", required = true)
            @Valid @RequestBody ContractRenewalRequest request) {
        log.info("Renewing contract: {}", contractId);
        ContractResponse response = contractService.renewContract(contractId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Cancel a contract",
        description = "Cancels an active or pending contract. Optionally cancels all unpaid dues. " +
                      "Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract cancelled successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - contract cannot be cancelled",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{contractId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ContractResponse> cancelContract(
            @Parameter(description = "Contract ID to cancel", required = true)
            @PathVariable Long contractId,
            @Parameter(description = "Cancellation details", required = true)
            @Valid @RequestBody ContractCancellationRequest request) {
        log.info("Cancelling contract: {}", contractId);
        ContractResponse response = contractService.cancelContract(contractId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Modify a contract",
        description = "Creates a new contract superseding the current one with modifications. " +
                      "Used for rent changes or other term modifications. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contract modified successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ContractResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Contract not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - contract cannot be modified",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{contractId}/modify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ContractResponse> modifyContract(
            @Parameter(description = "Contract ID to modify", required = true)
            @PathVariable Long contractId,
            @Parameter(description = "Modification details", required = true)
            @Valid @RequestBody ContractModificationRequest request) {
        log.info("Modifying contract: {}", contractId);
        ContractResponse response = contractService.modifyContract(contractId, request);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Get expiring contracts",
        description = "Retrieves contracts expiring within the specified number of days. " +
                      "Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Expiring contracts retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ContractSummaryResponse.class))
            )
        )
    })
    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ContractSummaryResponse>> getExpiringContracts(
            @Parameter(description = "Number of days to look ahead", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        log.info("Fetching contracts expiring within {} days", days);
        List<ContractSummaryResponse> contracts = contractService.getExpiringContracts(days);
        return ResponseEntity.ok(contracts);
    }
    
    @Operation(
        summary = "Get contracts with overdue payments",
        description = "Retrieves active contracts that have overdue monthly dues. " +
                      "Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contracts with overdue payments retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ContractSummaryResponse.class))
            )
        )
    })
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ContractSummaryResponse>> getContractsWithOverdueDues() {
        log.info("Fetching contracts with overdue payments");
        List<ContractSummaryResponse> contracts = contractService.getContractsWithOverdueDues();
        return ResponseEntity.ok(contracts);
    }
    
    @Operation(
        summary = "Get renewable contracts",
        description = "Retrieves contracts eligible for renewal (expiring soon with no overdue payments). " +
                      "Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Renewable contracts retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ContractSummaryResponse.class))
            )
        )
    })
    @GetMapping("/renewable")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ContractSummaryResponse>> getRenewableContracts(
            @Parameter(description = "Number of days ahead to check", example = "30")
            @RequestParam(defaultValue = "30") int daysAhead) {
        log.info("Fetching renewable contracts for next {} days", daysAhead);
        List<ContractSummaryResponse> contracts = contractService.getRenewableContracts(daysAhead);
        return ResponseEntity.ok(contracts);
    }
    
    @Operation(
        summary = "Get contract statistics",
        description = "Retrieves contract statistics for a building including counts by status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        )
    })
    @GetMapping("/building/{buildingId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getContractStatistics(
            @Parameter(description = "Building ID", required = true)
            @PathVariable Long buildingId) {
        log.info("Fetching contract statistics for building: {}", buildingId);
        Map<String, Object> statistics = contractService.getContractStatisticsByBuilding(buildingId);
        return ResponseEntity.ok(statistics);
    }
    
    @Operation(
        summary = "Get total monthly rent",
        description = "Calculates total monthly rent from all active contracts in a building."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Total monthly rent calculated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        )
    })
    @GetMapping("/building/{buildingId}/monthly-rent")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalMonthlyRent(
            @Parameter(description = "Building ID", required = true)
            @PathVariable Long buildingId) {
        log.info("Calculating total monthly rent for building: {}", buildingId);
        BigDecimal totalRent = contractService.getTotalMonthlyRentByBuilding(buildingId);
        return ResponseEntity.ok(Map.of("totalMonthlyRent", totalRent));
    }
    
    @Operation(
        summary = "Generate expiry notifications",
        description = "Manually triggers contract expiry notification generation. " +
                      "Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Notifications generated successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ContractExpiryNotification.class))
            )
        )
    })
    @PostMapping("/notifications/expiry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ContractExpiryNotification>> generateExpiryNotifications() {
        log.info("Generating contract expiry notifications");
        List<ContractExpiryNotification> notifications = contractService.generateExpiryNotifications();
        return ResponseEntity.ok(notifications);
    }
    
    @Operation(
        summary = "Update contract statuses",
        description = "Manually triggers contract status updates based on current date. " +
                      "Updates PENDING contracts to ACTIVE and ACTIVE contracts to EXPIRED as appropriate. " +
                      "Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Contract statuses updated successfully"
        )
    })
    @PostMapping("/update-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateContractStatuses() {
        log.info("Manually updating contract statuses");
        contractService.updateContractStatuses();
        return ResponseEntity.noContent().build();
    }
}