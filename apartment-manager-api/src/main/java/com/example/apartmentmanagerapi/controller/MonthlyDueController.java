package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.MonthlyDueRequest;
import com.example.apartmentmanagerapi.dto.MonthlyDueResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.mapper.MonthlyDueMapper;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import com.example.apartmentmanagerapi.service.IMonthlyDueService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing monthly dues.
 * Provides endpoints for creating, retrieving, and managing monthly dues for flats.
 * Includes bulk operations and debtor management features.
 */
@Tag(name = "Monthly Dues", description = "Manage monthly dues, debtor tracking, and bulk due generation")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/monthly-dues")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class MonthlyDueController {
    
    private final IMonthlyDueService monthlyDueService;
    private final FlatRepository flatRepository;
    private final MonthlyDueRepository monthlyDueRepository;
    private final MonthlyDueMapper monthlyDueMapper;
    
    @Operation(
        summary = "Generate monthly dues",
        description = "Generates monthly dues for all active flats in a building. Idempotent operation - duplicate dues are skipped. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Monthly dues generated successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MonthlyDueResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or building not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<MonthlyDueResponse>> generateMonthlyDues(
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Generating monthly dues for building ID: {} | Mode: {} | Amount: {} | Fallback: {}", 
                request.getBuildingId(), 
                Boolean.TRUE.equals(request.getUseFlatsMonthlyRent()) ? "FLAT_RENT_BASED" : "UNIFORM",
                request.getDueAmount(),
                request.getFallbackAmount());
        
        List<MonthlyDue> createdDues;
        
        // Check if we should use the enhanced method with rent-based generation
        if (Boolean.TRUE.equals(request.getUseFlatsMonthlyRent()) || 
            request.getFallbackAmount() != null) {
            // Use enhanced method
            createdDues = monthlyDueService.generateMonthlyDuesForBuilding(
                    request.getBuildingId(),
                    request.getDueAmount(),
                    request.getDueDate(),
                    request.getDueDescription(),
                    Boolean.TRUE.equals(request.getUseFlatsMonthlyRent()),
                    request.getFallbackAmount()
            );
        } else {
            // Use original method for backward compatibility
            createdDues = monthlyDueService.generateMonthlyDuesForBuilding(
                    request.getBuildingId(),
                    request.getDueAmount(),
                    request.getDueDate(),
                    request.getDueDescription()
            );
        }
        
        List<MonthlyDueResponse> responses = createdDues.stream()
                .map(monthlyDueMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    
    @Operation(
        summary = "Create single monthly due",
        description = "Creates a single monthly due for a specific flat. Used for custom charges or adjustments. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Monthly due created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MonthlyDueResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or flat not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - Due already exists for this flat and date"
        )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MonthlyDueResponse> createMonthlyDue(
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Creating monthly due for flat ID: {} with amount: {}", 
                request.getFlatId(), request.getDueAmount());
        
        // Get the flat
        Flat flat = flatRepository.findById(request.getFlatId())
                .orElseThrow(() -> new RuntimeException("Flat not found"));
        
        // Convert request to entity using mapper
        MonthlyDue monthlyDue = monthlyDueMapper.toEntity(request);
        monthlyDue.setFlat(flat); // Set the flat relationship
        
        MonthlyDue createdDue = monthlyDueService.createMonthlyDue(monthlyDue);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(monthlyDueMapper.toResponse(createdDue));
    }
    
    @Operation(
        summary = "Get monthly dues by flat",
        description = "Retrieves all monthly dues for a specific flat, ordered by due date descending. Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Monthly dues retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MonthlyDueResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/flat/{flatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<MonthlyDueResponse>> getMonthlyDuesByFlat(
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId) {
        log.info("Retrieving monthly dues for flat ID: {}", flatId);
        
        List<MonthlyDue> dues = monthlyDueService.getMonthlyDuesByFlat(flatId);
        List<MonthlyDueResponse> responses = dues.stream()
                .map(monthlyDueMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get debtors list",
        description = "Retrieves list of flats with overdue payments and their total debt amounts. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Debtors list retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = Map.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/building/{buildingId}/debtors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getDebtorsByBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        log.info("Retrieving debtors for building ID: {}", buildingId);
        
        Map<Flat, BigDecimal> debtorDetails = monthlyDueService.getDebtorDetailsForBuilding(buildingId);
        
        List<Map<String, Object>> debtorList = debtorDetails.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> debtor = new HashMap<>();
                    debtor.put("flatId", entry.getKey().getId());
                    debtor.put("flatNumber", entry.getKey().getFlatNumber());
                    debtor.put("tenantName", entry.getKey().getTenantName());
                    debtor.put("tenantContact", entry.getKey().getTenantContact());
                    debtor.put("totalDebt", entry.getValue());
                    return debtor;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(debtorList);
    }
    
    @Operation(
        summary = "Get overdue dues",
        description = "Retrieves all monthly dues with OVERDUE status for a building. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Overdue dues retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MonthlyDueResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/building/{buildingId}/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<MonthlyDueResponse>> getOverdueDuesByBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        log.info("Retrieving overdue dues for building ID: {}", buildingId);
        
        List<MonthlyDue> overdueDues = monthlyDueService.getOverdueDuesByBuilding(buildingId);
        List<MonthlyDueResponse> responses = overdueDues.stream()
                .map(monthlyDueMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get all monthly dues for a building",
        description = "Retrieves all monthly dues for a building regardless of status. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Monthly dues retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = MonthlyDueResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<MonthlyDueResponse>> getAllDuesForBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        log.info("Retrieving all monthly dues for building ID: {}", buildingId);
        
        List<MonthlyDue> allDues = monthlyDueService.getAllMonthlyDuesForBuilding(buildingId);
        List<MonthlyDueResponse> responses = allDues.stream()
                .map(monthlyDueMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get collection rate",
        description = "Calculates the percentage of monthly dues collected for a building within a date range. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Collection rate calculated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/building/{buildingId}/collection-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getCollectionRate(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Start date for collection rate calculation (defaults to first day of current month)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for collection rate calculation (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Calculating collection rate for building ID: {}", buildingId);
        
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        double collectionRate = monthlyDueService.getCollectionRate(buildingId, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("buildingId", buildingId);
        statistics.put("collectionRate", collectionRate);
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);
        
        return ResponseEntity.ok(statistics);
    }
    
    @Operation(
        summary = "Update monthly due",
        description = "Updates an existing monthly due's amount, description, or status. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Monthly due updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MonthlyDueResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or monthly due not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have ADMIN role"
        )
    })
    @PutMapping("/{dueId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MonthlyDueResponse> updateMonthlyDue(
            @Parameter(description = "ID of the monthly due to update", required = true)
            @PathVariable Long dueId,
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Updating monthly due ID: {}", dueId);
        
        // Get existing due first
        MonthlyDue existingDue = monthlyDueRepository.findById(dueId)
                .orElseThrow(() -> new RuntimeException("Monthly due not found"));
        
        // Update fields using mapper
        monthlyDueMapper.updateEntityFromRequest(request, existingDue);
        
        MonthlyDue updatedDue = monthlyDueService.updateMonthlyDue(existingDue);
        
        return ResponseEntity.ok(monthlyDueMapper.toResponse(updatedDue));
    }
    
    @Operation(
        summary = "Cancel monthly due",
        description = "Sets a monthly due status to CANCELLED. Cannot cancel already paid dues. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Monthly due cancelled successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot cancel a paid monthly due",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Monthly due not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have ADMIN role"
        )
    })
    @DeleteMapping("/{dueId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelMonthlyDue(
            @Parameter(description = "ID of the monthly due to cancel", required = true)
            @PathVariable Long dueId) {
        log.info("Cancelling monthly due ID: {}", dueId);
        
        monthlyDueService.cancelMonthlyDue(dueId);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
        summary = "Update overdue statuses",
        description = "Manually triggers the scheduled task to update unpaid dues to OVERDUE status. Normally runs daily at 01:00. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Overdue statuses updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have ADMIN role"
        )
    })
    @PostMapping("/update-overdue-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateOverdueStatuses() {
        log.info("Manually triggering overdue status update");
        
        monthlyDueService.updateOverdueStatuses();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Overdue statuses updated successfully");
        response.put("timestamp", LocalDate.now());
        
        return ResponseEntity.ok(response);
    }
}