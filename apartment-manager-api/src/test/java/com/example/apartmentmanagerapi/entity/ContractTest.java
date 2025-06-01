package com.example.apartmentmanagerapi.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Contract entity focusing on edge cases
 */
class ContractTest {

    private Contract contract;
    private Flat flat;
    private User tenant;

    @BeforeEach
    void setUp() {
        // Create test flat
        flat = new Flat();
        flat.setId(1L);
        flat.setFlatNumber("A101");
        
        // Create test tenant
        tenant = new User();
        tenant.setId(1L);
        tenant.setUsername("john.doe");
        
        // Create base contract
        contract = new Contract();
        contract.setFlat(flat);
        contract.setTenant(tenant);
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusYears(1));
        contract.setMonthlyRent(new BigDecimal("10000"));
        contract.setDayOfMonth(5);
        contract.setStatus(Contract.ContractStatus.ACTIVE);
    }

    @Nested
    @DisplayName("Entity State Tests")
    class EntityStateTests {
        
        @Test
        @DisplayName("Should create contract with all required fields")
        void shouldCreateContractWithAllRequiredFields() {
            // Assert
            assertThat(contract.getFlat()).isNotNull();
            assertThat(contract.getTenant()).isNotNull();
            assertThat(contract.getStartDate()).isNotNull();
            assertThat(contract.getEndDate()).isNotNull();
            assertThat(contract.getMonthlyRent()).isNotNull();
            assertThat(contract.getDayOfMonth()).isNotNull();
            assertThat(contract.getStatus()).isNotNull();
        }
        
        @Test
        @DisplayName("Should initialize with default values")
        void shouldInitializeWithDefaultValues() {
            // Arrange
            Contract newContract = new Contract();
            
            // Assert
            assertThat(newContract.getStatus()).isNull(); // Should be set by service
            assertThat(newContract.isAutoRenew()).isFalse();
            assertThat(newContract.isDuesGenerated()).isFalse();
            assertThat(newContract.getMonthlyDues()).isEmpty();
        }
        
        @Test
        @DisplayName("Should track creation and update timestamps")
        void shouldTrackTimestamps() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();
            contract.setCreatedAt(now);
            contract.setUpdatedAt(now.plusMinutes(10));
            
            // Assert
            assertThat(contract.getCreatedAt()).isEqualTo(now);
            assertThat(contract.getUpdatedAt()).isAfter(contract.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {
        
        @Test
        @DisplayName("Should validate end date is after start date")
        void shouldValidateEndDateAfterStartDate() {
            // Act & Assert
            assertThat(contract.getEndDate()).isAfter(contract.getStartDate());
        }
        
        @ParameterizedTest
        @ValueSource(ints = {1, 15, 28, 29, 30, 31})
        @DisplayName("Should accept valid day of month values")
        void shouldAcceptValidDayOfMonth(int dayOfMonth) {
            // Act
            contract.setDayOfMonth(dayOfMonth);
            
            // Assert
            assertThat(contract.getDayOfMonth()).isEqualTo(dayOfMonth);
        }
        
        @Test
        @DisplayName("Should handle February edge case for day 31")
        void shouldHandleFebruaryEdgeCase() {
            // Arrange
            contract.setDayOfMonth(31);
            contract.setStartDate(LocalDate.of(2024, 2, 1)); // Leap year
            
            // Act & Assert
            // This should be handled by service layer when generating dues
            assertThat(contract.getDayOfMonth()).isEqualTo(31);
        }
        
        @Test
        @DisplayName("Should calculate contract duration correctly")
        void shouldCalculateContractDuration() {
            // Arrange
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 12, 31);
            contract.setStartDate(start);
            contract.setEndDate(end);
            
            // Act
            long duration = contract.getEndDate().toEpochDay() - contract.getStartDate().toEpochDay();
            
            // Assert
            assertThat(duration).isEqualTo(365); // 2024 is a leap year
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {
        
        @ParameterizedTest
        @EnumSource(Contract.ContractStatus.class)
        @DisplayName("Should transition between valid statuses")
        void shouldTransitionBetweenStatuses(Contract.ContractStatus status) {
            // Act
            contract.setStatus(status);
            
            // Assert
            assertThat(contract.getStatus()).isEqualTo(status);
        }
        
        @Test
        @DisplayName("Should track status change metadata")
        void shouldTrackStatusChangeMetadata() {
            // Arrange
            LocalDateTime changedAt = LocalDateTime.now();
            String changedBy = "admin";
            String reason = "Contract expired";
            
            // Act
            contract.setStatus(Contract.ContractStatus.EXPIRED);
            contract.setStatusChangedAt(changedAt);
            contract.setStatusChangedBy(changedBy);
            contract.setStatusChangeReason(reason);
            
            // Assert
            assertThat(contract.getStatusChangedAt()).isEqualTo(changedAt);
            assertThat(contract.getStatusChangedBy()).isEqualTo(changedBy);
            assertThat(contract.getStatusChangeReason()).isEqualTo(reason);
        }
        
        @Test
        @DisplayName("Should identify active contract correctly")
        void shouldIdentifyActiveContract() {
            // Arrange
            contract.setStatus(Contract.ContractStatus.ACTIVE);
            
            // Act & Assert
            assertThat(contract.isActive()).isTrue();
            
            // Test other statuses
            contract.setStatus(Contract.ContractStatus.EXPIRED);
            assertThat(contract.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Financial Calculation Tests")
    class FinancialCalculationTests {
        
        @ParameterizedTest
        @CsvSource({
            "10000, 12, 120000",
            "15000.50, 6, 90003",
            "25000, 24, 600000",
            "0, 12, 0"
        })
        @DisplayName("Should calculate total contract value")
        void shouldCalculateTotalContractValue(String monthlyRent, int months, String expectedTotal) {
            // Arrange
            contract.setMonthlyRent(new BigDecimal(monthlyRent));
            contract.setStartDate(LocalDate.now());
            contract.setEndDate(LocalDate.now().plusMonths(months));
            
            // Act
            BigDecimal totalValue = contract.calculateTotalContractValue();
            
            // Assert
            assertThat(totalValue).isEqualByComparingTo(new BigDecimal(expectedTotal));
        }
        
        @Test
        @DisplayName("Should handle null deposit amount")
        void shouldHandleNullDeposit() {
            // Arrange
            contract.setDepositAmount(null);
            
            // Act & Assert
            assertThat(contract.getDepositAmount()).isNull();
            assertThat(contract.calculateTotalWithDeposit())
                .isEqualByComparingTo(contract.getMonthlyRent().multiply(BigDecimal.valueOf(12)));
        }
        
        @Test
        @DisplayName("Should calculate with deposit")
        void shouldCalculateWithDeposit() {
            // Arrange
            contract.setDepositAmount(new BigDecimal("20000"));
            
            // Act
            BigDecimal total = contract.calculateTotalWithDeposit();
            
            // Assert
            assertThat(total).isEqualByComparingTo(new BigDecimal("140000")); // 120000 + 20000
        }
    }

    @Nested
    @DisplayName("Contract History Tests")
    class ContractHistoryTests {
        
        @Test
        @DisplayName("Should link to previous contract for renewals")
        void shouldLinkToPreviousContract() {
            // Arrange
            Contract previousContract = new Contract();
            previousContract.setId(100L);
            previousContract.setStatus(Contract.ContractStatus.RENEWED);
            
            // Act
            contract.setPreviousContract(previousContract);
            
            // Assert
            assertThat(contract.getPreviousContract()).isNotNull();
            assertThat(contract.getPreviousContract().getId()).isEqualTo(100L);
        }
        
        @Test
        @DisplayName("Should maintain contract chain")
        void shouldMaintainContractChain() {
            // Arrange
            Contract original = new Contract();
            original.setId(1L);
            
            Contract renewal1 = new Contract();
            renewal1.setId(2L);
            renewal1.setPreviousContract(original);
            
            Contract renewal2 = new Contract();
            renewal2.setId(3L);
            renewal2.setPreviousContract(renewal1);
            
            // Act & Assert
            assertThat(renewal2.getPreviousContract()).isEqualTo(renewal1);
            assertThat(renewal2.getPreviousContract().getPreviousContract()).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("Monthly Due Relationship Tests")
    class MonthlyDueRelationshipTests {
        
        @Test
        @DisplayName("Should manage monthly dues collection")
        void shouldManageMonthlyDuesCollection() {
            // Arrange
            MonthlyDue due1 = new MonthlyDue();
            due1.setId(1L);
            due1.setDueAmount(new BigDecimal("10000"));
            
            MonthlyDue due2 = new MonthlyDue();
            due2.setId(2L);
            due2.setDueAmount(new BigDecimal("10000"));
            
            // Act
            contract.getMonthlyDues().add(due1);
            contract.getMonthlyDues().add(due2);
            
            // Assert
            assertThat(contract.getMonthlyDues()).hasSize(2);
            assertThat(contract.getMonthlyDues()).containsExactlyInAnyOrder(due1, due2);
        }
        
        @Test
        @DisplayName("Should calculate outstanding balance")
        void shouldCalculateOutstandingBalance() {
            // Arrange
            MonthlyDue paidDue = new MonthlyDue();
            paidDue.setDueAmount(new BigDecimal("10000"));
            paidDue.setStatus(MonthlyDue.DueStatus.PAID);
            
            MonthlyDue unpaidDue1 = new MonthlyDue();
            unpaidDue1.setDueAmount(new BigDecimal("10000"));
            unpaidDue1.setStatus(MonthlyDue.DueStatus.UNPAID);
            
            MonthlyDue unpaidDue2 = new MonthlyDue();
            unpaidDue2.setDueAmount(new BigDecimal("10000"));
            unpaidDue2.setStatus(MonthlyDue.DueStatus.UNPAID);
            
            contract.getMonthlyDues().addAll(Set.of(paidDue, unpaidDue1, unpaidDue2));
            
            // Act
            BigDecimal outstanding = contract.calculateOutstandingBalance();
            
            // Assert
            assertThat(outstanding).isEqualByComparingTo(new BigDecimal("20000"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle same day start and end")
        void shouldHandleSameDayStartAndEnd() {
            // Arrange
            LocalDate today = LocalDate.now();
            contract.setStartDate(today);
            contract.setEndDate(today);
            
            // Act & Assert
            assertThat(contract.getStartDate()).isEqualTo(contract.getEndDate());
            assertThat(contract.calculateTotalContractValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
        
        @Test
        @DisplayName("Should handle leap year February 29")
        void shouldHandleLeapYear() {
            // Arrange
            contract.setDayOfMonth(29);
            contract.setStartDate(LocalDate.of(2024, 2, 29)); // Leap year
            contract.setEndDate(LocalDate.of(2025, 2, 28)); // Non-leap year
            
            // Act & Assert
            assertThat(contract.getDayOfMonth()).isEqualTo(29);
            // Service layer should handle the adjustment
        }
        
        @Test
        @DisplayName("Should handle null tenant for vacant periods")
        void shouldHandleNullTenant() {
            // Arrange
            contract.setTenant(null);
            contract.setTenantName("Vacant");
            
            // Act & Assert
            assertThat(contract.getTenant()).isNull();
            assertThat(contract.getTenantName()).isEqualTo("Vacant");
        }
        
        @Test
        @DisplayName("Should handle zero monthly rent")
        void shouldHandleZeroMonthlyRent() {
            // Arrange
            contract.setMonthlyRent(BigDecimal.ZERO);
            
            // Act & Assert
            assertThat(contract.calculateTotalContractValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(contract.calculateOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        }
        
        @Test
        @DisplayName("Should handle very large contract values")
        void shouldHandleVeryLargeValues() {
            // Arrange
            BigDecimal largeRent = new BigDecimal("999999999.99");
            contract.setMonthlyRent(largeRent);
            contract.setStartDate(LocalDate.now());
            contract.setEndDate(LocalDate.now().plusYears(10)); // 120 months
            
            // Act
            BigDecimal total = contract.calculateTotalContractValue();
            
            // Assert
            assertThat(total).isGreaterThan(new BigDecimal("100000000000")); // > 100 billion
        }
    }

    @Nested
    @DisplayName("Business Rule Tests")
    class BusinessRuleTests {
        
        @Test
        @DisplayName("Should not allow modification after dues generated")
        void shouldNotAllowModificationAfterDuesGenerated() {
            // Arrange
            contract.setDuesGenerated(true);
            
            // Act & Assert
            assertThat(contract.isModifiable()).isFalse();
        }
        
        @Test
        @DisplayName("Should allow modification before dues generated")
        void shouldAllowModificationBeforeDuesGenerated() {
            // Arrange
            contract.setDuesGenerated(false);
            contract.setStatus(Contract.ContractStatus.PENDING);
            
            // Act & Assert
            assertThat(contract.isModifiable()).isTrue();
        }
        
        @Test
        @DisplayName("Should check if contract is expiring soon")
        void shouldCheckIfExpiringInDays() {
            // Arrange
            contract.setEndDate(LocalDate.now().plusDays(29));
            
            // Act & Assert
            assertThat(contract.isExpiringInDays(30)).isTrue();
            assertThat(contract.isExpiringInDays(28)).isFalse();
        }
        
        @Test
        @DisplayName("Should validate auto-renewal eligibility")
        void shouldValidateAutoRenewalEligibility() {
            // Arrange
            contract.setAutoRenew(true);
            contract.setStatus(Contract.ContractStatus.ACTIVE);
            contract.setEndDate(LocalDate.now().plusDays(15));
            
            // Act & Assert
            assertThat(contract.isEligibleForAutoRenewal()).isTrue();
            
            // Test with expired status
            contract.setStatus(Contract.ContractStatus.EXPIRED);
            assertThat(contract.isEligibleForAutoRenewal()).isFalse();
        }
    }
}