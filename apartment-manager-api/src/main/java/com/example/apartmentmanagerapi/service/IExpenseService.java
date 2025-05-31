package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing apartment building expenses.
 * Handles expense tracking, categorization, and analytics.
 */
public interface IExpenseService {

    /**
     * Creates a new expense for a building.
     * Optionally distributes the expense to all active flats.
     * 
     * @param expense the expense entity to create
     * @param distributeToFlats whether to distribute expense to flats as monthly dues
     * @return the created expense
     */
    Expense createExpense(Expense expense, boolean distributeToFlats);

    /**
     * Retrieves expenses for a building within a date range.
     * 
     * @param buildingId the building ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of expenses within the date range
     */
    List<Expense> getExpensesByBuildingAndDateRange(Long buildingId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves expenses for a building by category.
     * 
     * @param buildingId the building ID
     * @param category the expense category
     * @return list of expenses in the specified category
     */
    List<Expense> getExpensesByBuildingAndCategory(Long buildingId, Expense.ExpenseCategory category);

    /**
     * Retrieves all recurring expenses for a building.
     * 
     * @param buildingId the building ID
     * @return list of recurring expenses
     */
    List<Expense> getRecurringExpenses(Long buildingId);

    /**
     * Gets expense breakdown by category for a date range.
     * 
     * @param buildingId the building ID
     * @param startDate the start date
     * @param endDate the end date
     * @return map of categories to total amounts
     */
    Map<Expense.ExpenseCategory, BigDecimal> getExpenseBreakdownByCategory(Long buildingId, LocalDate startDate, LocalDate endDate);

    /**
     * Gets monthly expense totals for a period.
     * 
     * @param buildingId the building ID
     * @param startMonth the start month
     * @param endMonth the end month
     * @return map of months to total expense amounts
     */
    Map<YearMonth, BigDecimal> getMonthlyExpenseTotals(Long buildingId, YearMonth startMonth, YearMonth endMonth);

    /**
     * Calculates average monthly expenses over a period.
     * 
     * @param buildingId the building ID
     * @param months number of months to calculate
     * @return average monthly expense amount
     */
    BigDecimal calculateAverageMonthlyExpenses(Long buildingId, int months);

    /**
     * Analyzes expense trends for a building.
     * Provides comprehensive analytics including trends and patterns.
     * 
     * @param buildingId the building ID
     * @param periodDays number of days to analyze
     * @return map containing various analytical metrics
     */
    Map<String, Object> analyzeExpenseTrends(Long buildingId, int periodDays);

    /**
     * Updates an existing expense.
     * 
     * @param expense the expense entity with updates
     * @return the updated expense
     */
    Expense updateExpense(Expense expense);

    /**
     * Deletes an expense.
     * 
     * @param expenseId the expense ID to delete
     */
    void deleteExpense(Long expenseId);
}