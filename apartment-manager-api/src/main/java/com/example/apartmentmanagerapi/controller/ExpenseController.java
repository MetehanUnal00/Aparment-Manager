package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.ExpenseRequest;
import com.example.apartmentmanagerapi.dto.ExpenseResponse;
import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Expense;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.mapper.ExpenseMapper;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.ExpenseRepository;
import com.example.apartmentmanagerapi.repository.UserRepository;
import com.example.apartmentmanagerapi.service.IExpenseService;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing building expenses.
 * Provides endpoints for tracking, categorizing, and analyzing expenses.
 * Supports expense distribution to flats and trend analysis.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class ExpenseController {
    
    private final IExpenseService expenseService;
    private final ApartmentBuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseMapper expenseMapper;
    
    /**
     * Create a new expense for a building.
     * Optionally distributes the expense to all flats.
     * 
     * @param request Expense creation request
     * @param authentication Current user authentication
     * @return Created expense details
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            Authentication authentication) {
        
        log.info("Creating expense for building ID: {} with amount: {} in category: {}", 
                request.getBuildingId(), request.getAmount(), request.getCategory());
        
        // Get the building
        ApartmentBuilding building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new RuntimeException("Building not found"));
        
        // Get the current user
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Convert request to entity using mapper
        Expense expense = expenseMapper.toEntity(request);
        expense.setBuilding(building); // Set the building relationship
        expense.setRecordedBy(currentUser); // Set who recorded the expense
        
        // Create expense with optional distribution
        Expense createdExpense = expenseService.createExpense(expense, 
                request.getDistributeToFlats() != null && request.getDistributeToFlats());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseMapper.toResponse(createdExpense));
    }
    
    /**
     * Get expenses for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return List of expenses
     */
    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByBuilding(
            @PathVariable Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Retrieving expenses for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        
        List<Expense> expenses;
        
        if (startDate != null && endDate != null) {
            expenses = expenseService.getExpensesByBuildingAndDateRange(buildingId, startDate, endDate);
        } else {
            // Default to current month
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDate monthEnd = LocalDate.now();
            expenses = expenseService.getExpensesByBuildingAndDateRange(buildingId, monthStart, monthEnd);
        }
        
        List<ExpenseResponse> responses = expenses.stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get expenses by category for a building.
     * 
     * @param buildingId ID of the building
     * @param category Expense category
     * @return List of expenses in the category
     */
    @GetMapping("/building/{buildingId}/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(
            @PathVariable Long buildingId,
            @PathVariable Expense.ExpenseCategory category) {
        
        log.info("Retrieving expenses for building ID: {} in category: {}", buildingId, category);
        
        List<Expense> expenses = expenseService.getExpensesByBuildingAndCategory(buildingId, category);
        List<ExpenseResponse> responses = expenses.stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get expense breakdown by category for a building.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return Category breakdown with totals
     */
    @GetMapping("/building/{buildingId}/breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getExpenseBreakdown(
            @PathVariable Long buildingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("Getting expense breakdown for building ID: {}", buildingId);
        
        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        Map<Expense.ExpenseCategory, BigDecimal> breakdown = 
                expenseService.getExpenseBreakdownByCategory(buildingId, startDate, endDate);
        
        // Convert to response format
        List<Map<String, Object>> categoryBreakdown = breakdown.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("category", entry.getKey());
                    item.put("categoryName", entry.getKey().getDisplayName());
                    item.put("totalAmount", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", buildingId);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("breakdown", categoryBreakdown);
        response.put("totalExpenses", breakdown.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get monthly expense trends for a building.
     * 
     * @param buildingId ID of the building
     * @param months Number of months to analyze (default 6)
     * @return Monthly expense totals
     */
    @GetMapping("/building/{buildingId}/monthly-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends(
            @PathVariable Long buildingId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting monthly expense trends for building ID: {} for {} months", buildingId, months);
        
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months - 1);
        
        Map<YearMonth, BigDecimal> monthlyTotals = 
                expenseService.getMonthlyExpenseTotals(buildingId, startMonth, endMonth);
        
        List<Map<String, Object>> trends = monthlyTotals.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("month", entry.getKey().toString());
                    item.put("total", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        
        BigDecimal averageMonthlyExpense = expenseService.calculateAverageMonthlyExpenses(buildingId, months);
        
        Map<String, Object> response = new HashMap<>();
        response.put("buildingId", buildingId);
        response.put("monthlyTrends", trends);
        response.put("averageMonthlyExpense", averageMonthlyExpense);
        response.put("months", months);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get recurring expenses for a building.
     * 
     * @param buildingId ID of the building
     * @return List of recurring expenses
     */
    @GetMapping("/building/{buildingId}/recurring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ExpenseResponse>> getRecurringExpenses(@PathVariable Long buildingId) {
        log.info("Retrieving recurring expenses for building ID: {}", buildingId);
        
        List<Expense> recurringExpenses = expenseService.getRecurringExpenses(buildingId);
        List<ExpenseResponse> responses = recurringExpenses.stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Analyze expense trends comparing current and previous periods.
     * 
     * @param buildingId ID of the building
     * @param periodDays Number of days in each period (default 30)
     * @return Trend analysis results
     */
    @GetMapping("/building/{buildingId}/trend-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> analyzeExpenseTrends(
            @PathVariable Long buildingId,
            @RequestParam(defaultValue = "30") int periodDays) {
        
        log.info("Analyzing expense trends for building ID: {} with period of {} days", 
                buildingId, periodDays);
        
        Map<String, Object> trends = expenseService.analyzeExpenseTrends(buildingId, periodDays);
        
        return ResponseEntity.ok(trends);
    }
    
    /**
     * Update an expense.
     * Only ADMIN can update expenses.
     * 
     * @param expenseId ID of the expense to update
     * @param request Update request
     * @return Updated expense
     */
    @PutMapping("/{expenseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody ExpenseRequest request) {
        
        log.info("Updating expense ID: {}", expenseId);
        
        // Get existing expense first
        Expense existingExpense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        // Update fields using mapper
        expenseMapper.updateEntityFromRequest(request, existingExpense);
        
        Expense updatedExpense = expenseService.updateExpense(existingExpense);
        
        return ResponseEntity.ok(expenseMapper.toResponse(updatedExpense));
    }
    
    /**
     * Delete an expense.
     * Only ADMIN can delete expenses.
     * 
     * @param expenseId ID of the expense to delete
     * @return No content
     */
    @DeleteMapping("/{expenseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long expenseId) {
        log.info("Deleting expense ID: {}", expenseId);
        
        expenseService.deleteExpense(expenseId);
        
        return ResponseEntity.noContent().build();
    }
}