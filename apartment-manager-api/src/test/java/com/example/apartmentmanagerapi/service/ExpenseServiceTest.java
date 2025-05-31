package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Expense;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.ExpenseRecordedEvent;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.ExpenseRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpenseService
 * Tests expense creation, distribution, categorization, and analytics
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ApartmentBuildingRepository apartmentBuildingRepository;

    @Mock
    private FlatRepository flatRepository;

    @Mock
    private MonthlyDueService monthlyDueService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ExpenseService expenseService;

    private ApartmentBuilding testBuilding;
    private Expense testExpense;
    private Flat testFlat1;
    private Flat testFlat2;

    @BeforeEach
    void setUp() {
        // Initialize test building
        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test Street");

        // Initialize test expense
        testExpense = new Expense();
        testExpense.setBuilding(testBuilding);
        testExpense.setAmount(BigDecimal.valueOf(1000));
        testExpense.setCategory(Expense.ExpenseCategory.MAINTENANCE);
        testExpense.setDescription("Building maintenance");
        testExpense.setExpenseDate(LocalDate.now());
        testExpense.setVendorName("Test Vendor");
        testExpense.setInvoiceNumber("INV001");
        testExpense.setIsRecurring(false);

        // Initialize test flats
        testFlat1 = new Flat();
        testFlat1.setId(1L);
        testFlat1.setFlatNumber("101");
        testFlat1.setApartmentBuilding(testBuilding);
        testFlat1.setIsActive(true);

        testFlat2 = new Flat();
        testFlat2.setId(2L);
        testFlat2.setFlatNumber("102");
        testFlat2.setApartmentBuilding(testBuilding);
        testFlat2.setIsActive(true);
    }

    // Tests for createExpense method
    @Test
    @DisplayName("Create expense - Success without distribution")
    void createExpense_Success_WithoutDistribution() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);

        // Act
        Expense result = expenseService.createExpense(testExpense, false);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testExpense);

        // Verify interactions
        verify(apartmentBuildingRepository).findById(1L);
        verify(expenseRepository).save(testExpense);
        verify(flatRepository, never()).findByApartmentBuildingIdAndIsActiveTrue(anyLong());
        verify(monthlyDueService, never()).createMonthlyDue(any());

        // Verify event was published
        ArgumentCaptor<ExpenseRecordedEvent> eventCaptor = ArgumentCaptor.forClass(ExpenseRecordedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ExpenseRecordedEvent event = eventCaptor.getValue();
        assertThat(event.getBuildingId()).isEqualTo(1L);
        assertThat(event.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(event.getCategory()).isEqualTo(Expense.ExpenseCategory.MAINTENANCE);
        assertThat(event.isShouldDistributeToFlats()).isFalse();
    }

    @Test
    @DisplayName("Create expense - Success with distribution to flats")
    void createExpense_Success_WithDistribution() {
        // Arrange
        testExpense.setId(1L);
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2));

        // Act
        Expense result = expenseService.createExpense(testExpense, true);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testExpense);

        // Verify distribution occurred
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(1L);
        
        // Verify monthly dues were created for each flat
        ArgumentCaptor<MonthlyDue> dueCaptor = ArgumentCaptor.forClass(MonthlyDue.class);
        verify(monthlyDueService, times(2)).createMonthlyDue(dueCaptor.capture());
        
        List<MonthlyDue> capturedDues = dueCaptor.getAllValues();
        assertThat(capturedDues).hasSize(2);
        
        // Each flat should get 500 (1000 / 2 flats)
        assertThat(capturedDues.get(0).getDueAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(capturedDues.get(0).getDueDate()).isEqualTo(testExpense.getExpenseDate().plusDays(30));
        assertThat(capturedDues.get(0).getDueDescription()).contains("Maintenance expense");

        // Verify event
        ArgumentCaptor<ExpenseRecordedEvent> eventCaptor = ArgumentCaptor.forClass(ExpenseRecordedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().isShouldDistributeToFlats()).isTrue();
    }

    @Test
    @DisplayName("Create expense - Building not found throws exception")
    void createExpense_BuildingNotFound_ThrowsException() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.createExpense(testExpense, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Building not found with ID: 1");

        // Verify
        verify(apartmentBuildingRepository).findById(1L);
        verify(expenseRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Create expense - Distribution with no active flats")
    void createExpense_DistributionNoActiveFlats() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Collections.emptyList());

        // Act
        Expense result = expenseService.createExpense(testExpense, true);

        // Assert
        assertThat(result).isNotNull();
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(1L);
        verify(monthlyDueService, never()).createMonthlyDue(any());
    }

    // Tests for getExpensesByBuildingAndDateRange method
    @Test
    @DisplayName("Get expenses by building and date range - Success")
    void getExpensesByBuildingAndDateRange_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<Expense> expectedExpenses = Arrays.asList(testExpense);
        when(expenseRepository.findByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(expectedExpenses);

        // Act
        List<Expense> result = expenseService.getExpensesByBuildingAndDateRange(1L, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedExpenses);

        // Verify
        verify(expenseRepository).findByBuildingAndDateRange(1L, startDate, endDate);
    }

    // Tests for getExpensesByBuildingAndCategory method
    @Test
    @DisplayName("Get expenses by building and category - Success")
    void getExpensesByBuildingAndCategory_Success() {
        // Arrange
        List<Expense> expectedExpenses = Arrays.asList(testExpense);
        when(expenseRepository.findByBuildingAndCategory(1L, Expense.ExpenseCategory.MAINTENANCE))
                .thenReturn(expectedExpenses);

        // Act
        List<Expense> result = expenseService.getExpensesByBuildingAndCategory(
                1L, Expense.ExpenseCategory.MAINTENANCE);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedExpenses);

        // Verify
        verify(expenseRepository).findByBuildingAndCategory(1L, Expense.ExpenseCategory.MAINTENANCE);
    }

    // Tests for getExpenseBreakdownByCategory method
    @Test
    @DisplayName("Get expense breakdown by category - Success")
    void getExpenseBreakdownByCategory_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        Expense expense2 = new Expense();
        expense2.setCategory(Expense.ExpenseCategory.UTILITIES);
        expense2.setAmount(BigDecimal.valueOf(500));
        
        Expense expense3 = new Expense();
        expense3.setCategory(Expense.ExpenseCategory.MAINTENANCE);
        expense3.setAmount(BigDecimal.valueOf(300));
        
        List<Expense> expenses = Arrays.asList(testExpense, expense2, expense3);
        when(expenseRepository.findByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(expenses);

        // Act
        Map<Expense.ExpenseCategory, BigDecimal> result = 
                expenseService.getExpenseBreakdownByCategory(1L, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(Expense.ExpenseCategory.MAINTENANCE)).isEqualTo(BigDecimal.valueOf(1300));
        assertThat(result.get(Expense.ExpenseCategory.UTILITIES)).isEqualTo(BigDecimal.valueOf(500));
    }

    // Tests for getMonthlyExpenseTotals method
    @Test
    @DisplayName("Get monthly expense totals - Success")
    void getMonthlyExpenseTotals_Success() {
        // Arrange
        YearMonth startMonth = YearMonth.now().minusMonths(2);
        YearMonth endMonth = YearMonth.now();
        
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000), BigDecimal.valueOf(1500), BigDecimal.valueOf(1200));

        // Act
        Map<YearMonth, BigDecimal> result = 
                expenseService.getMonthlyExpenseTotals(1L, startMonth, endMonth);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(startMonth)).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.get(startMonth.plusMonths(1))).isEqualTo(BigDecimal.valueOf(1500));
        assertThat(result.get(endMonth)).isEqualTo(BigDecimal.valueOf(1200));

        // Verify
        verify(expenseRepository, times(3)).getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Get monthly expense totals - Null returns zero")
    void getMonthlyExpenseTotals_NullReturnsZero() {
        // Arrange
        YearMonth currentMonth = YearMonth.now();
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any()))
                .thenReturn(null);

        // Act
        Map<YearMonth, BigDecimal> result = 
                expenseService.getMonthlyExpenseTotals(1L, currentMonth, currentMonth);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(currentMonth)).isEqualTo(BigDecimal.ZERO);
    }

    // Tests for getRecurringExpenses method
    @Test
    @DisplayName("Get recurring expenses - Success")
    void getRecurringExpenses_Success() {
        // Arrange
        List<Expense> expectedExpenses = Arrays.asList(testExpense);
        when(expenseRepository.findByBuildingIdAndIsRecurringTrue(1L))
                .thenReturn(expectedExpenses);

        // Act
        List<Expense> result = expenseService.getRecurringExpenses(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedExpenses);

        // Verify
        verify(expenseRepository).findByBuildingIdAndIsRecurringTrue(1L);
    }

    // Tests for updateExpense method
    @Test
    @DisplayName("Update expense - Success")
    void updateExpense_Success() {
        // Arrange
        testExpense.setId(1L);
        Expense existingExpense = new Expense();
        existingExpense.setId(1L);
        existingExpense.setAmount(BigDecimal.valueOf(800));
        existingExpense.setCategory(Expense.ExpenseCategory.UTILITIES);
        
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(existingExpense));
        when(expenseRepository.save(any(Expense.class))).thenReturn(existingExpense);

        // Act
        Expense result = expenseService.updateExpense(testExpense);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isEqualTo(Expense.ExpenseCategory.MAINTENANCE);
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.getDescription()).isEqualTo("Building maintenance");

        // Verify
        verify(expenseRepository).findById(1L);
        verify(expenseRepository).save(existingExpense);
    }

    @Test
    @DisplayName("Update expense - Not found throws exception")
    void updateExpense_NotFound_ThrowsException() {
        // Arrange
        testExpense.setId(999L);
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.updateExpense(testExpense))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expense not found with ID: 999");

        // Verify
        verify(expenseRepository).findById(999L);
        verify(expenseRepository, never()).save(any());
    }

    // Tests for deleteExpense method
    @Test
    @DisplayName("Delete expense - Success")
    void deleteExpense_Success() {
        // Arrange
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(testExpense));

        // Act
        expenseService.deleteExpense(1L);

        // Assert & Verify
        verify(expenseRepository).findById(1L);
        verify(expenseRepository).delete(testExpense);
    }

    @Test
    @DisplayName("Delete expense - Not found throws exception")
    void deleteExpense_NotFound_ThrowsException() {
        // Arrange
        when(expenseRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> expenseService.deleteExpense(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expense not found with ID: 999");

        // Verify
        verify(expenseRepository).findById(999L);
        verify(expenseRepository, never()).delete(any());
    }

    // Tests for calculateAverageMonthlyExpenses method
    @Test
    @DisplayName("Calculate average monthly expenses - Success")
    void calculateAverageMonthlyExpenses_Success() {
        // Arrange
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any()))
                .thenReturn(BigDecimal.valueOf(6000));

        // Act
        BigDecimal result = expenseService.calculateAverageMonthlyExpenses(1L, 3);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000));

        // Verify
        verify(expenseRepository).getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Calculate average monthly expenses - No expenses returns zero")
    void calculateAverageMonthlyExpenses_NoExpenses_ReturnsZero() {
        // Arrange
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any()))
                .thenReturn(null);

        // Act
        BigDecimal result = expenseService.calculateAverageMonthlyExpenses(1L, 3);

        // Assert
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    // Tests for analyzeExpenseTrends method
    @Test
    @DisplayName("Analyze expense trends - Increasing trend")
    void analyzeExpenseTrends_IncreasingTrend() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate currentPeriodStart = today.minusDays(30);
        LocalDate previousPeriodStart = currentPeriodStart.minusDays(30);
        
        // First call for current period
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, currentPeriodStart, today))
                .thenReturn(BigDecimal.valueOf(1200));
        // Second call for previous period
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, previousPeriodStart, currentPeriodStart))
                .thenReturn(BigDecimal.valueOf(1000));

        // Act
        Map<String, Object> result = expenseService.analyzeExpenseTrends(1L, 30);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("currentPeriodTotal")).isEqualTo(BigDecimal.valueOf(1200));
        assertThat(result.get("previousPeriodTotal")).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.get("changeAmount")).isEqualTo(BigDecimal.valueOf(200));
        assertThat(result.get("trend")).isEqualTo("INCREASING");

        // Verify
        verify(expenseRepository, times(2)).getTotalExpensesByBuildingAndDateRange(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Analyze expense trends - Decreasing trend")
    void analyzeExpenseTrends_DecreasingTrend() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate currentPeriodStart = today.minusDays(30);
        LocalDate previousPeriodStart = currentPeriodStart.minusDays(30);
        
        // First call for current period
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, currentPeriodStart, today))
                .thenReturn(BigDecimal.valueOf(800));
        // Second call for previous period
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, previousPeriodStart, currentPeriodStart))
                .thenReturn(BigDecimal.valueOf(1000));

        // Act
        Map<String, Object> result = expenseService.analyzeExpenseTrends(1L, 30);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("currentPeriodTotal")).isEqualTo(BigDecimal.valueOf(800));
        assertThat(result.get("previousPeriodTotal")).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.get("changeAmount")).isEqualTo(BigDecimal.valueOf(-200));
        assertThat(result.get("trend")).isEqualTo("DECREASING");
    }

    @Test
    @DisplayName("Analyze expense trends - Stable (no previous data)")
    void analyzeExpenseTrends_Stable_NoPreviousData() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate currentPeriodStart = today.minusDays(30);
        LocalDate previousPeriodStart = currentPeriodStart.minusDays(30);
        
        // First call for current period
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, currentPeriodStart, today))
                .thenReturn(BigDecimal.valueOf(1000));
        // Second call for previous period returns null
        when(expenseRepository.getTotalExpensesByBuildingAndDateRange(1L, previousPeriodStart, currentPeriodStart))
                .thenReturn(null);

        // Act
        Map<String, Object> result = expenseService.analyzeExpenseTrends(1L, 30);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.get("currentPeriodTotal")).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.get("previousPeriodTotal")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("changePercentage")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("trend")).isEqualTo("STABLE");
    }

    // Tests for expense distribution logic
    @Test
    @DisplayName("Distribute expense - Three flats with rounding")
    void distributeExpense_ThreeFlats_WithRounding() {
        // Arrange
        Flat testFlat3 = new Flat();
        testFlat3.setId(3L);
        testFlat3.setFlatNumber("103");
        testFlat3.setIsActive(true);
        
        testExpense.setAmount(BigDecimal.valueOf(1000)); // 333.33 per flat
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(expenseRepository.save(any(Expense.class))).thenReturn(testExpense);
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2, testFlat3));

        // Act
        expenseService.createExpense(testExpense, true);

        // Assert
        ArgumentCaptor<MonthlyDue> dueCaptor = ArgumentCaptor.forClass(MonthlyDue.class);
        verify(monthlyDueService, times(3)).createMonthlyDue(dueCaptor.capture());
        
        List<MonthlyDue> capturedDues = dueCaptor.getAllValues();
        // Each flat should get 333.33
        assertThat(capturedDues.get(0).getDueAmount()).isEqualTo(BigDecimal.valueOf(333.33));
    }
}