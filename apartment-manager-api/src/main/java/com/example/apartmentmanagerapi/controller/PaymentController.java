package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.PaymentRequest;
import com.example.apartmentmanagerapi.dto.PaymentResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.Payment;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.mapper.PaymentMapper;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.UserRepository;
import com.example.apartmentmanagerapi.service.IPaymentService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing apartment payments.
 * Provides endpoints for creating, retrieving, and managing payments.
 * Access is restricted based on user roles.
 */
@Tag(name = "Payments", description = "Manage apartment payments and transactions")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {
    
    private final IPaymentService paymentService;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    
    @Operation(
        summary = "Create a payment",
        description = "Records a new payment for a flat. Automatically allocates payment to oldest unpaid monthly dues. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Payment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors, flat not found, or payment exceeds outstanding balance",
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
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
        log.info("Creating payment for flat ID: {} by user: {}", 
                request.getFlatId(), authentication.getName());
        
        // Get the flat
        Flat flat = flatRepository.findById(request.getFlatId())
                .orElseThrow(() -> new RuntimeException("Flat not found"));
        
        // Get the current user
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Build payment entity using mapper
        Payment payment = paymentMapper.toEntity(request);
        payment.setFlat(flat);
        payment.setRecordedBy(currentUser);
        
        // Set default payment date if not provided
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }
        
        // Create payment
        Payment createdPayment = paymentService.createPayment(payment);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentMapper.toResponse(createdPayment));
    }
    
    @Operation(
        summary = "Get payments by flat",
        description = "Retrieves all payments for a specific flat, ordered by payment date descending. Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class))
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
    public ResponseEntity<List<PaymentResponse>> getPaymentsByFlat(
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId) {
        log.info("Retrieving payments for flat ID: {}", flatId);
        
        List<Payment> payments = paymentService.getPaymentsByFlat(flatId);
        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get payments by building",
        description = "Retrieves all payments for a building within a date range. Defaults to current month if dates not provided. Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class))
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
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Start date for filtering (defaults to first day of current month)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Retrieving payments for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        List<Payment> payments = paymentService.getPaymentsByBuildingAndDateRange(
                buildingId, startDate, endDate);
        
        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get payment statistics",
        description = "Retrieves payment statistics for a building including total amount, count, and average. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
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
    @GetMapping("/building/{buildingId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Start date for statistics (defaults to first day of current month)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for statistics (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Retrieving payment statistics for building ID: {}", buildingId);
        
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        BigDecimal totalAmount = paymentService.getTotalPaymentsByBuildingAndDateRange(
                buildingId, startDate, endDate);
        
        List<Payment> payments = paymentService.getPaymentsByBuildingAndDateRange(
                buildingId, startDate, endDate);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAmount", totalAmount);
        statistics.put("paymentCount", payments.size());
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);
        statistics.put("averagePayment", payments.isEmpty() ? BigDecimal.ZERO : 
                totalAmount.divide(BigDecimal.valueOf(payments.size()), 2, RoundingMode.HALF_UP));
        
        return ResponseEntity.ok(statistics);
    }
    
    @Operation(
        summary = "Get outstanding balance",
        description = "Calculates the current outstanding balance for a flat (unpaid monthly dues). Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Balance calculated successfully",
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
    @GetMapping("/flat/{flatId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getOutstandingBalance(
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId) {
        log.info("Calculating outstanding balance for flat ID: {}", flatId);
        
        BigDecimal balance = paymentService.calculateOutstandingBalance(flatId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("flatId", flatId);
        response.put("outstandingBalance", balance);
        response.put("calculatedAt", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "Update payment",
        description = "Updates payment method, description, or receipt number. Amount cannot be changed after creation. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or payment not found",
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
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - Optimistic locking failure"
        )
    })
    @PutMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> updatePayment(
            @Parameter(description = "ID of the payment to update", required = true)
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentRequest request) {
        
        log.info("Updating payment ID: {}", paymentId);
        
        // Build payment entity with only the fields that can be updated
        Payment payment = Payment.builder()
                .id(paymentId)
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription())
                .receiptNumber(request.getReceiptNumber())
                .build();
        
        Payment updatedPayment = paymentService.updatePayment(payment);
        
        return ResponseEntity.ok(paymentMapper.toResponse(updatedPayment));
    }
    
    @Operation(
        summary = "Delete payment",
        description = "Deletes a payment and reverses allocations. Consider soft delete for audit trail. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Payment deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
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
    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(
            @Parameter(description = "ID of the payment to delete", required = true)
            @PathVariable Long paymentId) {
        log.info("Deleting payment ID: {}", paymentId);
        
        paymentService.deletePayment(paymentId);
        
        return ResponseEntity.noContent().build();
    }
}