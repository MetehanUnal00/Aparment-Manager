package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.MonthlyDueRequest;
import com.example.apartmentmanagerapi.dto.MonthlyDueResponse;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.service.MonthlyDueService;
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
@RestController
@RequestMapping("/api/monthly-dues")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class MonthlyDueController {
    
    private final MonthlyDueService monthlyDueService;
    private final FlatRepository flatRepository;
    
    /**
     * Generate monthly dues for all flats in a building.
     * Only ADMIN and MANAGER roles can generate dues.
     * 
     * @param request Monthly due generation request
     * @return List of created monthly dues
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<MonthlyDueResponse>> generateMonthlyDues(
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Generating monthly dues for building ID: {} with amount: {}", 
                request.getBuildingId(), request.getDueAmount());
        
        List<MonthlyDue> createdDues = monthlyDueService.generateMonthlyDuesForBuilding(
                request.getBuildingId(),
                request.getDueAmount(),
                request.getDueDate(),
                request.getDueDescription()
        );
        
        List<MonthlyDueResponse> responses = createdDues.stream()
                .map(MonthlyDueResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }
    
    /**
     * Create a single monthly due for a specific flat.
     * Used for custom charges or adjustments.
     * 
     * @param request Monthly due creation request
     * @return Created monthly due
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MonthlyDueResponse> createMonthlyDue(
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Creating monthly due for flat ID: {} with amount: {}", 
                request.getFlatId(), request.getDueAmount());
        
        // Get the flat
        Flat flat = flatRepository.findById(request.getFlatId())
                .orElseThrow(() -> new RuntimeException("Flat not found"));
        
        // Build monthly due entity
        MonthlyDue monthlyDue = MonthlyDue.builder()
                .flat(flat)
                .dueAmount(request.getDueAmount())
                .dueDate(request.getDueDate())
                .dueDescription(request.getDueDescription())
                .baseRent(request.getBaseRent())
                .additionalCharges(request.getAdditionalCharges())
                .additionalChargesDescription(request.getAdditionalChargesDescription())
                .status(MonthlyDue.DueStatus.UNPAID)
                .paidAmount(BigDecimal.ZERO)
                .build();
        
        MonthlyDue createdDue = monthlyDueService.createMonthlyDue(monthlyDue);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MonthlyDueResponse.fromEntity(createdDue));
    }
    
    /**
     * Get monthly dues for a specific flat.
     * 
     * @param flatId ID of the flat
     * @return List of monthly dues
     */
    @GetMapping("/flat/{flatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<MonthlyDueResponse>> getMonthlyDuesByFlat(@PathVariable Long flatId) {
        log.info("Retrieving monthly dues for flat ID: {}", flatId);
        
        List<MonthlyDue> dues = monthlyDueService.getMonthlyDuesByFlat(flatId);
        List<MonthlyDueResponse> responses = dues.stream()
                .map(MonthlyDueResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get debtors list for a building.
     * Returns flats with overdue payments.
     * 
     * @param buildingId ID of the building
     * @return List of debtors with their debt details
     */
    @GetMapping("/building/{buildingId}/debtors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getDebtorsByBuilding(@PathVariable Long buildingId) {
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
    
    /**
     * Get overdue dues for a building.
     * 
     * @param buildingId ID of the building
     * @return List of overdue monthly dues
     */
    @GetMapping("/building/{buildingId}/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<MonthlyDueResponse>> getOverdueDuesByBuilding(@PathVariable Long buildingId) {
        log.info("Retrieving overdue dues for building ID: {}", buildingId);
        
        List<MonthlyDue> overdueDues = monthlyDueService.getOverdueDuesByBuilding(buildingId);
        List<MonthlyDueResponse> responses = overdueDues.stream()
                .map(MonthlyDueResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get collection rate statistics for a building.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return Collection rate statistics
     */
    @GetMapping("/building/{buildingId}/collection-rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getCollectionRate(
            @PathVariable Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
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
    
    /**
     * Update a monthly due.
     * Only ADMIN can update dues.
     * 
     * @param dueId ID of the monthly due to update
     * @param request Update request
     * @return Updated monthly due
     */
    @PutMapping("/{dueId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MonthlyDueResponse> updateMonthlyDue(
            @PathVariable Long dueId,
            @Valid @RequestBody MonthlyDueRequest request) {
        
        log.info("Updating monthly due ID: {}", dueId);
        
        // Build monthly due entity with updated fields
        MonthlyDue monthlyDue = MonthlyDue.builder()
                .id(dueId)
                .dueAmount(request.getDueAmount())
                .dueDescription(request.getDueDescription())
                .build();
        
        MonthlyDue updatedDue = monthlyDueService.updateMonthlyDue(monthlyDue);
        
        return ResponseEntity.ok(MonthlyDueResponse.fromEntity(updatedDue));
    }
    
    /**
     * Cancel a monthly due.
     * Only ADMIN can cancel dues.
     * 
     * @param dueId ID of the monthly due to cancel
     * @return No content
     */
    @DeleteMapping("/{dueId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelMonthlyDue(@PathVariable Long dueId) {
        log.info("Cancelling monthly due ID: {}", dueId);
        
        monthlyDueService.cancelMonthlyDue(dueId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Manually trigger overdue status update.
     * Only ADMIN can trigger this operation.
     * 
     * @return Update statistics
     */
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