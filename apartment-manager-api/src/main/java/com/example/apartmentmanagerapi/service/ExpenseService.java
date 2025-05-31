package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Expense;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.ExpenseRecordedEvent;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.ExpenseRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing apartment building expenses.
 * Handles expense tracking, categorization, and distribution among flats.
 * Supports both one-time and recurring expenses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExpenseService implements IExpenseService {
    
    private final ExpenseRepository expenseRepository;
    private final ApartmentBuildingRepository apartmentBuildingRepository;
    private final FlatRepository flatRepository;
    private final MonthlyDueService monthlyDueService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Creates a new expense for a building.
     * If marked for distribution, automatically creates monthly dues for flats.
     * 
     * @param expense Expense entity to create
     * @param distributeToFlats Whether to distribute expense among flats
     * @return Created expense
     */
    @Caching(evict = {
        @CacheEvict(value = "monthlyExpenseTotals", key = "#expense.building.id"),
        @CacheEvict(value = "expenseCategoryBreakdown", key = "#expense.building.id"),
        @CacheEvict(value = "buildingFinancials", key = "#expense.building.id")
    })
    public Expense createExpense(Expense expense, boolean distributeToFlats) {
        log.info("Creating expense for building ID: {} with amount: {} in category: {}", 
                expense.getBuilding().getId(), expense.getAmount(), expense.getCategory());
        
        // Validate building exists
        ApartmentBuilding building = apartmentBuildingRepository
                .findById(expense.getBuilding().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Building not found with ID: " + expense.getBuilding().getId()));
        
        expense.setBuilding(building);
        
        // Save the expense
        Expense savedExpense = expenseRepository.save(expense);
        
        // Distribute to flats if requested
        if (distributeToFlats) {
            distributeExpenseToFlats(savedExpense);
        }
        
        // Publish expense recorded event
        ExpenseRecordedEvent event = new ExpenseRecordedEvent(
            this,
            savedExpense.getId(),
            building.getId(),
            savedExpense.getAmount(),
            savedExpense.getCategory(),
            savedExpense.getExpenseDate(),
            savedExpense.getIsRecurring(),
            distributeToFlats
        );
        eventPublisher.publishEvent(event);
        log.debug("Published ExpenseRecordedEvent for expense {}", savedExpense.getId());
        
        log.info("Expense created successfully with ID: {}", savedExpense.getId());
        return savedExpense;
    }
    
    /**
     * Distributes an expense equally among all active flats in the building.
     * Creates monthly dues for each flat's share of the expense.
     * 
     * @param expense Expense to distribute
     */
    private void distributeExpenseToFlats(Expense expense) {
        log.debug("Distributing expense ID: {} to flats", expense.getId());
        
        // Get all active flats in the building
        List<Flat> activeFlats = flatRepository
                .findByApartmentBuildingIdAndIsActiveTrue(expense.getBuilding().getId());
        
        if (activeFlats.isEmpty()) {
            log.warn("No active flats found for expense distribution in building ID: {}", 
                    expense.getBuilding().getId());
            return;
        }
        
        // Calculate amount per flat (equal distribution)
        BigDecimal amountPerFlat = expense.getAmount()
                .divide(BigDecimal.valueOf(activeFlats.size()), 2, RoundingMode.HALF_UP);
        
        // Create monthly due for each flat
        LocalDate dueDate = expense.getExpenseDate().plusDays(30); // 30 days to pay
        String description = String.format("%s expense: %s", 
                expense.getCategory().getDisplayName(), expense.getDescription());
        
        for (Flat flat : activeFlats) {
            MonthlyDue monthlyDue = MonthlyDue.builder()
                    .flat(flat)
                    .dueAmount(amountPerFlat)
                    .dueDate(dueDate)
                    .dueDescription(description)
                    .status(MonthlyDue.DueStatus.UNPAID)
                    .paidAmount(BigDecimal.ZERO)
                    .build();
            
            monthlyDueService.createMonthlyDue(monthlyDue);
        }
        
        log.info("Distributed expense ID: {} to {} flats with {} per flat", 
                expense.getId(), activeFlats.size(), amountPerFlat);
    }
    
    /**
     * Retrieves expenses for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of expenses within the date range
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByBuildingAndDateRange(
            Long buildingId, LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving expenses for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        return expenseRepository.findByBuildingAndDateRange(buildingId, startDate, endDate);
    }
    
    /**
     * Retrieves expenses for a building by category.
     * 
     * @param buildingId ID of the building
     * @param category Expense category
     * @return List of expenses in the specified category
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByBuildingAndCategory(
            Long buildingId, Expense.ExpenseCategory category) {
        log.debug("Retrieving expenses for building ID: {} in category: {}", 
                buildingId, category);
        return expenseRepository.findByBuildingAndCategory(buildingId, category);
    }
    
    /**
     * Calculates total expenses by category for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return Map of category to total amount
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "expenseCategoryBreakdown", key = "#buildingId + '-' + #startDate + '-' + #endDate")
    public Map<Expense.ExpenseCategory, BigDecimal> getExpenseBreakdownByCategory(
            Long buildingId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating expense breakdown for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        
        List<Expense> expenses = getExpensesByBuildingAndDateRange(buildingId, startDate, endDate);
        
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Expense::getAmount,
                                BigDecimal::add
                        )
                ));
    }
    
    /**
     * Calculates monthly expense totals for a building over multiple months.
     * 
     * @param buildingId ID of the building
     * @param startMonth Start month
     * @param endMonth End month
     * @return Map of YearMonth to total expenses
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "monthlyExpenseTotals", key = "#buildingId + '-' + #startMonth + '-' + #endMonth")
    public Map<YearMonth, BigDecimal> getMonthlyExpenseTotals(
            Long buildingId, YearMonth startMonth, YearMonth endMonth) {
        log.debug("Calculating monthly expense totals for building ID: {} from {} to {}", 
                buildingId, startMonth, endMonth);
        
        Map<YearMonth, BigDecimal> monthlyTotals = new HashMap<>();
        YearMonth currentMonth = startMonth;
        
        while (!currentMonth.isAfter(endMonth)) {
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            
            BigDecimal monthTotal = expenseRepository
                    .getTotalExpensesByBuildingAndDateRange(buildingId, monthStart, monthEnd);
            
            monthlyTotals.put(currentMonth, monthTotal != null ? monthTotal : BigDecimal.ZERO);
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return monthlyTotals;
    }
    
    /**
     * Retrieves recurring expenses for a building.
     * 
     * @param buildingId ID of the building
     * @return List of recurring expenses
     */
    @Transactional(readOnly = true)
    public List<Expense> getRecurringExpenses(Long buildingId) {
        log.debug("Retrieving recurring expenses for building ID: {}", buildingId);
        return expenseRepository.findByBuildingIdAndIsRecurringTrue(buildingId);
    }
    
    /**
     * Updates an expense.
     * Note: Changing amount after distribution would require recalculation.
     * 
     * @param expense Expense with updates
     * @return Updated expense
     */
    @Caching(evict = {
        @CacheEvict(value = "monthlyExpenseTotals", allEntries = true),
        @CacheEvict(value = "expenseCategoryBreakdown", allEntries = true),
        @CacheEvict(value = "buildingFinancials", allEntries = true)
    })
    public Expense updateExpense(Expense expense) {
        log.info("Updating expense ID: {}", expense.getId());
        
        Expense existingExpense = expenseRepository.findById(expense.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Expense not found with ID: " + expense.getId()));
        
        // Update allowed fields
        existingExpense.setCategory(expense.getCategory());
        existingExpense.setAmount(expense.getAmount());
        existingExpense.setDescription(expense.getDescription());
        existingExpense.setVendorName(expense.getVendorName());
        existingExpense.setInvoiceNumber(expense.getInvoiceNumber());
        existingExpense.setIsRecurring(expense.getIsRecurring());
        
        // Note: If amount changes after distribution, we'd need to:
        // 1. Track which monthly dues were created from this expense
        // 2. Update or recreate those dues with new amounts
        // For MVP, we'll log a warning
        
        if (!existingExpense.getAmount().equals(expense.getAmount())) {
            log.warn("Expense amount changed after potential distribution. " +
                    "Manual adjustment of monthly dues may be required.");
        }
        
        return expenseRepository.save(existingExpense);
    }
    
    /**
     * Deletes an expense.
     * Note: Associated monthly dues are not automatically deleted.
     * 
     * @param expenseId ID of the expense to delete
     */
    @Caching(evict = {
        @CacheEvict(value = "monthlyExpenseTotals", allEntries = true),
        @CacheEvict(value = "expenseCategoryBreakdown", allEntries = true),
        @CacheEvict(value = "buildingFinancials", allEntries = true)
    })
    public void deleteExpense(Long expenseId) {
        log.info("Deleting expense ID: {}", expenseId);
        
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Expense not found with ID: " + expenseId));
        
        // Note: In production, consider:
        // 1. Soft delete for audit trail
        // 2. Check if monthly dues were created and handle appropriately
        
        expenseRepository.delete(expense);
        log.info("Expense ID: {} deleted successfully", expenseId);
    }
    
    /**
     * Calculates average monthly expenses for a building.
     * 
     * @param buildingId ID of the building
     * @param months Number of months to average
     * @return Average monthly expense amount
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "buildingFinancials", key = "'avg-expense-' + #buildingId + '-' + #months")
    public BigDecimal calculateAverageMonthlyExpenses(Long buildingId, int months) {
        log.debug("Calculating average monthly expenses for building ID: {} over {} months", 
                buildingId, months);
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months).withDayOfMonth(1);
        
        BigDecimal totalExpenses = expenseRepository
                .getTotalExpensesByBuildingAndDateRange(buildingId, startDate, endDate);
        
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalExpenses.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Finds expense trends by comparing current period with previous period.
     * 
     * @param buildingId ID of the building
     * @param periodDays Number of days in each period to compare
     * @return Map with trend information (current, previous, change percentage)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "buildingFinancials", key = "'expense-trends-' + #buildingId + '-' + #periodDays")
    public Map<String, Object> analyzeExpenseTrends(Long buildingId, int periodDays) {
        log.debug("Analyzing expense trends for building ID: {} with period of {} days", 
                buildingId, periodDays);
        
        LocalDate today = LocalDate.now();
        LocalDate currentPeriodStart = today.minusDays(periodDays);
        LocalDate previousPeriodStart = currentPeriodStart.minusDays(periodDays);
        
        // Get totals for both periods
        BigDecimal currentTotal = expenseRepository
                .getTotalExpensesByBuildingAndDateRange(buildingId, currentPeriodStart, today);
        BigDecimal previousTotal = expenseRepository
                .getTotalExpensesByBuildingAndDateRange(buildingId, previousPeriodStart, currentPeriodStart);
        
        currentTotal = currentTotal != null ? currentTotal : BigDecimal.ZERO;
        previousTotal = previousTotal != null ? previousTotal : BigDecimal.ZERO;
        
        // Calculate change percentage
        BigDecimal changePercentage = BigDecimal.ZERO;
        if (previousTotal.compareTo(BigDecimal.ZERO) > 0) {
            changePercentage = currentTotal.subtract(previousTotal)
                    .divide(previousTotal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("currentPeriodTotal", currentTotal);
        trends.put("previousPeriodTotal", previousTotal);
        trends.put("changeAmount", currentTotal.subtract(previousTotal));
        trends.put("changePercentage", changePercentage);
        trends.put("trend", changePercentage.compareTo(BigDecimal.ZERO) > 0 ? "INCREASING" : 
                           changePercentage.compareTo(BigDecimal.ZERO) < 0 ? "DECREASING" : "STABLE");
        
        return trends;
    }
}