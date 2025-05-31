package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.ExpenseRequest;
import com.example.apartmentmanagerapi.dto.ExpenseResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Expense;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.mapper.ExpenseMapper;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.ExpenseRepository;
import com.example.apartmentmanagerapi.repository.UserRepository;
import com.example.apartmentmanagerapi.service.IExpenseService;
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
@Tag(name = "Expenses", description = "Manage building expenses, categories, and distribution")
@SecurityRequirement(name = "bearerAuth")
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
    
    @Operation(
        summary = "Create expense",
        description = "Records a new expense for a building. Optionally distributes the expense equally among all active flats. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Expense created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpenseResponse.class)
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
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ExpenseResponse> createExpense(
            @Valid @RequestBody ExpenseRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        
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
    
    @Operation(
        summary = "Get expenses by building",
        description = "Retrieves all expenses for a building within a date range. Defaults to current month if dates not provided. Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Expenses retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ExpenseResponse.class))
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
    public ResponseEntity<List<ExpenseResponse>> getExpensesByBuilding(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Start date for filtering (defaults to first day of current month)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (defaults to today)")
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
    
    @Operation(
        summary = "Get expenses by category",
        description = "Retrieves all expenses for a specific category in a building. Requires ADMIN, MANAGER, or VIEWER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Expenses retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ExpenseResponse.class))
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
    @GetMapping("/building/{buildingId}/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'VIEWER')")
    public ResponseEntity<List<ExpenseResponse>> getExpensesByCategory(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Expense category", required = true)
            @PathVariable Expense.ExpenseCategory category) {
        
        log.info("Retrieving expenses for building ID: {} in category: {}", buildingId, category);
        
        List<Expense> expenses = expenseService.getExpensesByBuildingAndCategory(buildingId, category);
        List<ExpenseResponse> responses = expenses.stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Get expense breakdown",
        description = "Provides expense breakdown by category with totals for a building. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Expense breakdown retrieved successfully",
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
    @GetMapping("/building/{buildingId}/breakdown")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getExpenseBreakdown(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Start date for breakdown (defaults to first day of current month)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for breakdown (defaults to today)")
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
    
    @Operation(
        summary = "Get monthly expense trends",
        description = "Analyzes monthly expense trends with totals and averages. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Monthly trends retrieved successfully",
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
    @GetMapping("/building/{buildingId}/monthly-trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getMonthlyTrends(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Number of months to analyze", example = "6")
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
    
    @Operation(
        summary = "Get recurring expenses",
        description = "Retrieves all expenses marked as recurring for a building. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recurring expenses retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ExpenseResponse.class))
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
    @GetMapping("/building/{buildingId}/recurring")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ExpenseResponse>> getRecurringExpenses(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId) {
        log.info("Retrieving recurring expenses for building ID: {}", buildingId);
        
        List<Expense> recurringExpenses = expenseService.getRecurringExpenses(buildingId);
        List<ExpenseResponse> responses = recurringExpenses.stream()
                .map(expenseMapper::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @Operation(
        summary = "Analyze expense trends",
        description = "Compares expenses between current and previous periods to identify trends. Requires ADMIN or MANAGER role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Trend analysis completed successfully",
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
    @GetMapping("/building/{buildingId}/trend-analysis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> analyzeExpenseTrends(
            @Parameter(description = "ID of the building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "Number of days in each comparison period", example = "30")
            @RequestParam(defaultValue = "30") int periodDays) {
        
        log.info("Analyzing expense trends for building ID: {} with period of {} days", 
                buildingId, periodDays);
        
        Map<String, Object> trends = expenseService.analyzeExpenseTrends(buildingId, periodDays);
        
        return ResponseEntity.ok(trends);
    }
    
    @Operation(
        summary = "Update expense",
        description = "Updates an existing expense. Changes to amount after distribution may require manual adjustment of monthly dues. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Expense updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExpenseResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or expense not found",
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
    @PutMapping("/{expenseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @Parameter(description = "ID of the expense to update", required = true)
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
    
    @Operation(
        summary = "Delete expense",
        description = "Deletes an expense. Associated monthly dues are not automatically deleted. Requires ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Expense deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Expense not found",
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
    @DeleteMapping("/{expenseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpense(
            @Parameter(description = "ID of the expense to delete", required = true)
            @PathVariable Long expenseId) {
        log.info("Deleting expense ID: {}", expenseId);
        
        expenseService.deleteExpense(expenseId);
        
        return ResponseEntity.noContent().build();
    }
}