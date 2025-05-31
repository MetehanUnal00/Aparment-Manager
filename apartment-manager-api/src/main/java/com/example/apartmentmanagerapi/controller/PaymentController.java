package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.PaymentRequest;
import com.example.apartmentmanagerapi.dto.PaymentResponse;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.Payment;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.mapper.PaymentMapper;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.UserRepository;
import com.example.apartmentmanagerapi.service.IPaymentService;
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
    
    /**
     * Create a new payment for a flat.
     * Only ADMIN and MANAGER roles can create payments.
     * 
     * @param request Payment creation request
     * @param authentication Current user authentication
     * @return Created payment details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        
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
    
    /**
     * Get payments for a specific flat.
     * Managers can only view payments for flats in their assigned buildings.
     * 
     * @param flatId ID of the flat
     * @return List of payments for the flat
     */
    @GetMapping("/flat/{flatId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByFlat(@PathVariable Long flatId) {
        log.info("Retrieving payments for flat ID: {}", flatId);
        
        List<Payment> payments = paymentService.getPaymentsByFlat(flatId);
        List<PaymentResponse> responses = payments.stream()
                .map(paymentMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get payments for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return List of payments
     */
    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByBuilding(
            @PathVariable Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
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
    
    /**
     * Get payment statistics for a building.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return Payment statistics including total amount and count
     */
    @GetMapping("/building/{buildingId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics(
            @PathVariable Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
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
    
    /**
     * Get outstanding balance for a flat.
     * 
     * @param flatId ID of the flat
     * @return Outstanding balance amount
     */
    @GetMapping("/flat/{flatId}/balance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<Map<String, Object>> getOutstandingBalance(@PathVariable Long flatId) {
        log.info("Calculating outstanding balance for flat ID: {}", flatId);
        
        BigDecimal balance = paymentService.calculateOutstandingBalance(flatId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("flatId", flatId);
        response.put("outstandingBalance", balance);
        response.put("calculatedAt", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update a payment (limited to certain fields).
     * Only ADMIN can update payments.
     * 
     * @param paymentId ID of the payment to update
     * @param request Update request
     * @return Updated payment
     */
    @PutMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> updatePayment(
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
    
    /**
     * Delete a payment.
     * Only ADMIN can delete payments.
     * 
     * @param paymentId ID of the payment to delete
     * @return No content
     */
    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long paymentId) {
        log.info("Deleting payment ID: {}", paymentId);
        
        paymentService.deletePayment(paymentId);
        
        return ResponseEntity.noContent().build();
    }
}