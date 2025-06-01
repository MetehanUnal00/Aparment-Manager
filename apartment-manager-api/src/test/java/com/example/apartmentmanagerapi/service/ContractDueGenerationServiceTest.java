package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.*;
import com.example.apartmentmanagerapi.exception.BusinessRuleException;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Contract Due Generation Service with edge cases
 */
@ExtendWith(MockitoExtension.class)
class ContractDueGenerationServiceTest {

    @Mock
    private MonthlyDueRepository monthlyDueRepository;
    
    @Mock
    private ContractRepository contractRepository;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private IAuditService auditService;
    
    @Captor
    private ArgumentCaptor<List<MonthlyDue>> monthlyDuesCaptor;
    
    private ContractDueGenerationService dueGenerationService;
    
    private Contract testContract;
    private Flat testFlat;
    private ApartmentBuilding testBuilding;

    @BeforeEach
    void setUp() {
        dueGenerationService = new ContractDueGenerationService(
            monthlyDueRepository,
            contractRepository,
            eventPublisher,
            auditService
        );
        
        // Setup test data
        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        
        testFlat = new Flat();
        testFlat.setId(1L);
        testFlat.setFlatNumber("A101");
        testFlat.setApartmentBuilding(testBuilding);
        
        testContract = Contract.builder()
            .id(1L)
            .flat(testFlat)
            .startDate(LocalDate.of(2024, 1, 15))
            .endDate(LocalDate.of(2024, 12, 15))
            .monthlyRent(new BigDecimal("10000"))
            .dayOfMonth(15)
            .status(Contract.ContractStatus.ACTIVE)
            .duesGenerated(false)
            .build();
    }

    @Nested
    @DisplayName("Basic Due Generation Tests")
    class BasicDueGenerationTests {
        
        @Test
        @DisplayName("Should generate monthly dues for full year contract")
        void shouldGenerateMonthlyDuesForFullYear() {
            // Arrange
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> generatedDues = dueGenerationService
                .generateDuesForContract(testContract);
            
            // Assert
            assertThat(generatedDues).hasSize(12);
            assertThat(generatedDues).allSatisfy(due -> {
                assertThat(due.getFlat()).isEqualTo(testFlat);
                assertThat(due.getAmount()).isEqualByComparingTo("10000");
                assertThat(due.isPaid()).isFalse();
                assertThat(due.getPaymentStatus()).isEqualTo(MonthlyDue.PaymentStatus.PENDING);
                assertThat(due.getDescription()).contains("Contract");
            });
            
            // Verify due dates
            assertThat(generatedDues.get(0).getDueDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(generatedDues.get(11).getDueDate()).isEqualTo(LocalDate.of(2024, 12, 15));
            
            // Verify contract marked as dues generated
            verify(contractRepository).save(argThat(contract -> 
                contract.isDuesGenerated()));
        }
        
        @Test
        @DisplayName("Should handle partial month at start")
        void shouldHandlePartialMonthAtStart() {
            // Arrange
            testContract.setStartDate(LocalDate.of(2024, 1, 25));
            testContract.setDayOfMonth(5); // Due on 5th of each month
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> generatedDues = dueGenerationService
                .generateDuesForContract(testContract);
            
            // Assert
            // First due should be on Feb 5 (skipping Jan since start is after due date)
            assertThat(generatedDues.get(0).getDueDate()).isEqualTo(LocalDate.of(2024, 2, 5));
        }
        
        @Test
        @DisplayName("Should prevent duplicate due generation")
        void shouldPreventDuplicateDueGeneration() {
            // Arrange
            testContract.setDuesGenerated(true);
            
            // Act & Assert
            assertThatThrownBy(() -> dueGenerationService.generateDuesForContract(testContract))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Dues already generated");
        }
    }

    @Nested
    @DisplayName("Day of Month Edge Cases")
    class DayOfMonthEdgeCases {
        
        @ParameterizedTest
        @CsvSource({
            "31, 2024-01-31, 2024-02-29, 2024-03-31", // Jan 31 -> Feb 29 (leap) -> Mar 31
            "31, 2023-01-31, 2023-02-28, 2023-03-31", // Jan 31 -> Feb 28 (non-leap) -> Mar 31
            "30, 2024-01-30, 2024-02-29, 2024-03-30", // Jan 30 -> Feb 29 (leap) -> Mar 30
            "29, 2024-01-29, 2024-02-29, 2024-03-29", // Jan 29 -> Feb 29 -> Mar 29
            "29, 2023-01-29, 2023-02-28, 2023-03-29"  // Jan 29 -> Feb 28 (non-leap) -> Mar 29
        })
        @DisplayName("Should handle month-end adjustments correctly")
        void shouldHandleMonthEndAdjustments(int dayOfMonth, String jan, String feb, String mar) {
            // Arrange
            testContract.setDayOfMonth(dayOfMonth);
            testContract.setStartDate(LocalDate.parse(jan).withDayOfMonth(1));
            testContract.setEndDate(LocalDate.parse(mar).plusMonths(1));
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> generatedDues = dueGenerationService
                .generateDuesForContract(testContract);
            
            // Assert
            assertThat(generatedDues.get(0).getDueDate()).isEqualTo(LocalDate.parse(jan));
            assertThat(generatedDues.get(1).getDueDate()).isEqualTo(LocalDate.parse(feb));
            assertThat(generatedDues.get(2).getDueDate()).isEqualTo(LocalDate.parse(mar));
        }
        
        @Test
        @DisplayName("Should handle February 29 in leap year")
        void shouldHandleFebruary29InLeapYear() {
            // Arrange
            testContract.setDayOfMonth(29);
            testContract.setStartDate(LocalDate.of(2024, 2, 1)); // Leap year
            testContract.setEndDate(LocalDate.of(2025, 2, 28)); // Non-leap year
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> generatedDues = dueGenerationService
                .generateDuesForContract(testContract);
            
            // Assert
            // Feb 2024 (leap year) - should be 29th
            MonthlyDue feb2024Due = generatedDues.stream()
                .filter(due -> due.getDueDate().getMonth().getValue() == 2 && 
                              due.getDueDate().getYear() == 2024)
                .findFirst().orElseThrow();
            assertThat(feb2024Due.getDueDate()).isEqualTo(LocalDate.of(2024, 2, 29));
            
            // Feb 2025 (non-leap year) - should be 28th
            MonthlyDue feb2025Due = generatedDues.stream()
                .filter(due -> due.getDueDate().getMonth().getValue() == 2 && 
                              due.getDueDate().getYear() == 2025)
                .findFirst().orElseThrow();
            assertThat(feb2025Due.getDueDate()).isEqualTo(LocalDate.of(2025, 2, 28));
        }
    }

    @Nested
    @DisplayName("Contract Extension Tests")
    class ContractExtensionTests {
        
        @Test
        @DisplayName("Should generate dues for contract extension")
        void shouldGenerateDuesForContractExtension() {
            // Arrange
            LocalDate extensionStart = LocalDate.of(2025, 1, 15);
            testContract.setEndDate(LocalDate.of(2025, 12, 15));
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> extensionDues = dueGenerationService
                .generateDuesForContractExtension(testContract, extensionStart);
            
            // Assert
            assertThat(extensionDues).hasSize(12);
            assertThat(extensionDues.get(0).getDueDate()).isEqualTo(extensionStart);
            assertThat(extensionDues.get(11).getDueDate()).isEqualTo(LocalDate.of(2025, 12, 15));
        }
        
        @Test
        @DisplayName("Should handle extension with different rent amount")
        void shouldHandleExtensionWithDifferentRent() {
            // Arrange
            LocalDate extensionStart = testContract.getEndDate().plusDays(1);
            Contract extensionContract = Contract.builder()
                .id(2L)
                .flat(testFlat)
                .startDate(extensionStart)
                .endDate(extensionStart.plusYears(1))
                .monthlyRent(new BigDecimal("12000")) // Increased rent
                .dayOfMonth(testContract.getDayOfMonth())
                .status(Contract.ContractStatus.ACTIVE)
                .previousContract(testContract)
                .build();
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> extensionDues = dueGenerationService
                .generateDuesForContractExtension(extensionContract, extensionStart);
            
            // Assert
            assertThat(extensionDues).allSatisfy(due -> 
                assertThat(due.getDueAmount()).isEqualByComparingTo("12000"));
        }
    }

    @Nested
    @DisplayName("Due Cancellation Tests")
    class DueCancellationTests {
        
        @Test
        @DisplayName("Should cancel unpaid dues for contract")
        void shouldCancelUnpaidDuesForContract() {
            // Arrange
            List<MonthlyDue> unpaidDues = List.of(
                createMonthlyDue(1L, false, MonthlyDue.PaymentStatus.PENDING),
                createMonthlyDue(2L, false, MonthlyDue.PaymentStatus.OVERDUE),
                createMonthlyDue(3L, true, MonthlyDue.PaymentStatus.PAID) // Should not be cancelled
            );
            
            when(monthlyDueRepository.findByContractId(1L)).thenReturn(unpaidDues);
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            int cancelledCount = dueGenerationService.cancelUnpaidDuesForContract(testContract);
            
            // Assert
            assertThat(cancelledCount).isEqualTo(2);
            verify(monthlyDueRepository).saveAll(monthlyDuesCaptor.capture());
            
            List<MonthlyDue> savedDues = monthlyDuesCaptor.getValue();
            assertThat(savedDues).hasSize(2);
            assertThat(savedDues).allSatisfy(due -> {
                assertThat(due.getPaymentStatus()).isEqualTo(MonthlyDue.PaymentStatus.CANCELLED);
                assertThat(due.getDescription()).contains("Cancelled due to contract cancellation");
            });
        }
        
        @Test
        @DisplayName("Should handle no unpaid dues")
        void shouldHandleNoUnpaidDues() {
            // Arrange
            List<MonthlyDue> allPaidDues = List.of(
                createMonthlyDue(1L, true, MonthlyDue.PaymentStatus.PAID),
                createMonthlyDue(2L, true, MonthlyDue.PaymentStatus.PAID)
            );
            
            when(monthlyDueRepository.findByContractId(1L)).thenReturn(allPaidDues);
            
            // Act
            int cancelledCount = dueGenerationService.cancelUnpaidDuesForContract(testContract);
            
            // Assert
            assertThat(cancelledCount).isEqualTo(0);
            verify(monthlyDueRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Modified Contract Due Regeneration Tests")
    class ModifiedContractDueRegenerationTests {
        
        @Test
        @DisplayName("Should regenerate dues from effective date")
        void shouldRegenerateDuesFromEffectiveDate() {
            // Arrange
            Contract oldContract = testContract;
            Contract newContract = Contract.builder()
                .id(2L)
                .flat(testFlat)
                .startDate(oldContract.getStartDate())
                .endDate(oldContract.getEndDate())
                .monthlyRent(new BigDecimal("12000")) // Changed rent
                .dayOfMonth(20) // Changed due date
                .status(Contract.ContractStatus.ACTIVE)
                .previousContract(oldContract)
                .build();
            
            LocalDate effectiveDate = LocalDate.of(2024, 6, 1);
            
            // Existing dues
            List<MonthlyDue> existingDues = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                MonthlyDue due = createMonthlyDue(
                    (long) i, 
                    i < 6, // First 5 months paid
                    i < 6 ? MonthlyDue.PaymentStatus.PAID : MonthlyDue.PaymentStatus.PENDING
                );
                due.setDueDate(LocalDate.of(2024, i, 15));
                existingDues.add(due);
            }
            
            when(monthlyDueRepository.findByContractId(1L)).thenReturn(existingDues);
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            dueGenerationService.regenerateDuesForModifiedContract(
                oldContract, newContract, effectiveDate);
            
            // Assert
            // Should delete unpaid dues from June onwards
            verify(monthlyDueRepository).deleteAll(argThat((List<MonthlyDue> dues) -> 
                dues.size() == 7 && dues.stream().allMatch(due -> !due.isPaid())));
            
            // Should create new dues with new amount and day
            verify(monthlyDueRepository).saveAll(argThat((List<MonthlyDue> dues) -> 
                dues.size() == 7 && 
                dues.stream().allMatch(due -> 
                    due.getAmount().compareTo(new BigDecimal("12000")) == 0 &&
                    due.getDueDate().getDayOfMonth() == 20)));
        }
        
        @Test
        @DisplayName("Should not modify paid dues")
        void shouldNotModifyPaidDues() {
            // Arrange
            Contract newContract = Contract.builder()
                .id(2L)
                .flat(testFlat)
                .monthlyRent(new BigDecimal("15000"))
                .dayOfMonth(10)
                .build();
            
            List<MonthlyDue> allPaidDues = List.of(
                createMonthlyDue(1L, true, MonthlyDue.PaymentStatus.PAID),
                createMonthlyDue(2L, true, MonthlyDue.PaymentStatus.PAID)
            );
            
            when(monthlyDueRepository.findByContractId(1L)).thenReturn(allPaidDues);
            
            // Act
            dueGenerationService.regenerateDuesForModifiedContract(
                testContract, newContract, LocalDate.now());
            
            // Assert
            verify(monthlyDueRepository, never()).deleteAll(anyList());
            verify(monthlyDueRepository, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Edge Case Scenarios")
    class EdgeCaseScenarios {
        
        @Test
        @DisplayName("Should handle same-day contract")
        void shouldHandleSameDayContract() {
            // Arrange
            LocalDate today = LocalDate.now();
            testContract.setStartDate(today);
            testContract.setEndDate(today);
            testContract.setDayOfMonth(today.getDayOfMonth());
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> dues = dueGenerationService.generateDuesForContract(testContract);
            
            // Assert
            assertThat(dues).hasSize(1);
            assertThat(dues.get(0).getDueDate()).isEqualTo(today);
        }
        
        @Test
        @DisplayName("Should handle zero rent contract")
        void shouldHandleZeroRentContract() {
            // Arrange
            testContract.setMonthlyRent(BigDecimal.ZERO);
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> dues = dueGenerationService.generateDuesForContract(testContract);
            
            // Assert
            assertThat(dues).hasSize(12);
            assertThat(dues).allSatisfy(due -> 
                assertThat(due.getDueAmount()).isEqualByComparingTo(BigDecimal.ZERO));
        }
        
        @ParameterizedTest
        @MethodSource("provideYearBoundaryScenarios")
        @DisplayName("Should handle year boundary scenarios")
        void shouldHandleYearBoundaryScenarios(LocalDate start, LocalDate end, 
                                              int expectedDues) {
            // Arrange
            testContract.setStartDate(start);
            testContract.setEndDate(end);
            testContract.setDayOfMonth(15);
            
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> 
                invocation.getArgument(0));
            
            // Act
            List<MonthlyDue> dues = dueGenerationService.generateDuesForContract(testContract);
            
            // Assert
            assertThat(dues).hasSize(expectedDues);
        }
        
        private static Stream<Arguments> provideYearBoundaryScenarios() {
            return Stream.of(
                // Cross year boundary
                Arguments.of(
                    LocalDate.of(2023, 12, 15),
                    LocalDate.of(2024, 1, 15),
                    2
                ),
                // Multiple year contract
                Arguments.of(
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2025, 12, 31),
                    36
                ),
                // Leap year February
                Arguments.of(
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 3, 1),
                    2
                )
            );
        }
        
        @Test
        @DisplayName("Should handle concurrent due generation attempts")
        void shouldHandleConcurrentDueGenerationAttempts() {
            // This simulates a race condition where dues might be generated twice
            
            // Arrange
            when(monthlyDueRepository.saveAll(anyList())).thenAnswer(invocation -> {
                // Simulate that dues were generated by another thread
                testContract.setDuesGenerated(true);
                return invocation.getArgument(0);
            });
            
            // First call should succeed
            List<MonthlyDue> firstAttempt = dueGenerationService
                .generateDuesForContract(testContract);
            assertThat(firstAttempt).isNotEmpty();
            
            // Second call should fail
            assertThatThrownBy(() -> dueGenerationService.generateDuesForContract(testContract))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Dues already generated");
        }
    }
    
    // Helper method to create test MonthlyDue
    private MonthlyDue createMonthlyDue(Long id, boolean paid, MonthlyDue.PaymentStatus status) {
        MonthlyDue due = new MonthlyDue();
        due.setId(id);
        due.setFlat(testFlat);
        due.setDueAmount(new BigDecimal("10000"));
        due.setDueDate(LocalDate.now());
        due.setStatus(paid ? MonthlyDue.DueStatus.PAID : MonthlyDue.DueStatus.UNPAID);
        due.setPaymentStatus(status);
        return due;
    }
}