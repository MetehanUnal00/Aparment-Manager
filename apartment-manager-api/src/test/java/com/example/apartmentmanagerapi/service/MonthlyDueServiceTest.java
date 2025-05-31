package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.MonthlyDuesGeneratedEvent;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MonthlyDueService
 * Tests monthly due generation, debtor tracking, and scheduled tasks
 */
@ExtendWith(MockitoExtension.class)
class MonthlyDueServiceTest {

    @Mock
    private MonthlyDueRepository monthlyDueRepository;

    @Mock
    private FlatRepository flatRepository;

    @Mock
    private ApartmentBuildingRepository apartmentBuildingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MonthlyDueService monthlyDueService;

    private ApartmentBuilding testBuilding;
    private Flat testFlat1;
    private Flat testFlat2;
    private MonthlyDue testDue1;
    private MonthlyDue testDue2;

    @BeforeEach
    void setUp() {
        // Initialize test building with default monthly fee
        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test Street");
        testBuilding.setDefaultMonthlyFee(BigDecimal.valueOf(1000));

        // Initialize test flats
        testFlat1 = new Flat();
        testFlat1.setId(1L);
        testFlat1.setFlatNumber("101");
        testFlat1.setApartmentBuilding(testBuilding);
        testFlat1.setTenantName("John Doe");
        testFlat1.setIsActive(true);

        testFlat2 = new Flat();
        testFlat2.setId(2L);
        testFlat2.setFlatNumber("102");
        testFlat2.setApartmentBuilding(testBuilding);
        testFlat2.setTenantName("Jane Doe");
        testFlat2.setIsActive(true);

        // Initialize test monthly dues
        testDue1 = new MonthlyDue();
        testDue1.setId(1L);
        testDue1.setFlat(testFlat1);
        testDue1.setDueAmount(BigDecimal.valueOf(1000));
        testDue1.setDueDate(LocalDate.now().minusMonths(1));
        testDue1.setStatus(MonthlyDue.DueStatus.UNPAID);
        testDue1.setPaidAmount(BigDecimal.ZERO);

        testDue2 = new MonthlyDue();
        testDue2.setId(2L);
        testDue2.setFlat(testFlat2);
        testDue2.setDueAmount(BigDecimal.valueOf(1000));
        testDue2.setDueDate(LocalDate.now());
        testDue2.setStatus(MonthlyDue.DueStatus.UNPAID);
        testDue2.setPaidAmount(BigDecimal.ZERO);
    }

    // Tests for generateMonthlyDuesForBuilding method
    @Test
    @DisplayName("Generate monthly dues for building - Success")
    void generateMonthlyDuesForBuilding_Success() {
        // Arrange
        LocalDate dueDate = LocalDate.now().plusDays(15);
        BigDecimal dueAmount = BigDecimal.valueOf(1000);
        String description = "Monthly maintenance fee";
        
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2));
        when(monthlyDueRepository.save(any(MonthlyDue.class)))
                .thenAnswer(invocation -> {
                    MonthlyDue due = invocation.getArgument(0);
                    due.setId((long) (Math.random() * 1000));
                    return due;
                });

        // Act
        List<MonthlyDue> result = monthlyDueService.generateMonthlyDuesForBuilding(
                1L, dueAmount, dueDate, description);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDueAmount()).isEqualTo(dueAmount);
        assertThat(result.get(0).getDueDate()).isEqualTo(dueDate);
        assertThat(result.get(0).getDueDescription()).isEqualTo(description);
        assertThat(result.get(0).getStatus()).isEqualTo(MonthlyDue.DueStatus.UNPAID);

        // Verify interactions
        verify(apartmentBuildingRepository).findById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(1L);
        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
        
        // Verify event was published
        ArgumentCaptor<MonthlyDuesGeneratedEvent> eventCaptor = 
                ArgumentCaptor.forClass(MonthlyDuesGeneratedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MonthlyDuesGeneratedEvent event = eventCaptor.getValue();
        assertThat(event.getBuildingId()).isEqualTo(1L);
        assertThat(event.getNumberOfFlatsAffected()).isEqualTo(2);
        assertThat(event.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    @DisplayName("Generate monthly dues for building - Building not found")
    void generateMonthlyDuesForBuilding_BuildingNotFound() {
        // Arrange
        when(apartmentBuildingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> monthlyDueService.generateMonthlyDuesForBuilding(
                999L, BigDecimal.valueOf(1000), LocalDate.now(), "Test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Building not found with ID: 999");

        // Verify
        verify(apartmentBuildingRepository).findById(999L);
        verify(flatRepository, never()).findByApartmentBuildingIdAndIsActiveTrue(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Generate monthly dues for building - Idempotency with duplicates")
    void generateMonthlyDuesForBuilding_IdempotencyWithDuplicates() {
        // Arrange
        LocalDate dueDate = LocalDate.now().plusDays(15);
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2));
        
        // First flat saves successfully, second throws duplicate exception
        when(monthlyDueRepository.save(any(MonthlyDue.class)))
                .thenAnswer(invocation -> {
                    MonthlyDue due = invocation.getArgument(0);
                    if (due.getFlat().getId().equals(1L)) {
                        due.setId(1L);
                        return due;
                    } else {
                        throw new DataIntegrityViolationException("Duplicate key");
                    }
                });

        // Act
        List<MonthlyDue> result = monthlyDueService.generateMonthlyDuesForBuilding(
                1L, BigDecimal.valueOf(1000), dueDate, "Test");

        // Assert
        assertThat(result).hasSize(1); // Only one due created
        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
    }

    @Test
    @DisplayName("Generate monthly dues for building - No active flats")
    void generateMonthlyDuesForBuilding_NoActiveFlats() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Collections.emptyList());

        // Act
        List<MonthlyDue> result = monthlyDueService.generateMonthlyDuesForBuilding(
                1L, BigDecimal.valueOf(1000), LocalDate.now(), "Test");

        // Assert
        assertThat(result).isEmpty();
        verify(eventPublisher, never()).publishEvent(any()); // No event for empty generation
    }

    // Tests for automatic generation scheduled task
    @Test
    @DisplayName("Generate monthly dues automatically - Success")
    void generateMonthlyDuesAutomatically_Success() {
        // Arrange
        when(apartmentBuildingRepository.findAll()).thenReturn(Collections.singletonList(testBuilding));
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2));
        when(monthlyDueRepository.save(any(MonthlyDue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        monthlyDueService.generateMonthlyDuesAutomatically();

        // Assert
        verify(apartmentBuildingRepository).findAll();
        verify(apartmentBuildingRepository).findById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(1L);
        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
        verify(eventPublisher).publishEvent(any(MonthlyDuesGeneratedEvent.class));
    }

    @Test
    @DisplayName("Generate monthly dues automatically - Skip building with no fee")
    void generateMonthlyDuesAutomatically_SkipBuildingWithNoFee() {
        // Arrange
        testBuilding.setDefaultMonthlyFee(null);
        when(apartmentBuildingRepository.findAll()).thenReturn(Collections.singletonList(testBuilding));

        // Act
        monthlyDueService.generateMonthlyDuesAutomatically();

        // Assert
        verify(apartmentBuildingRepository).findAll();
        verify(monthlyDueRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Generate monthly dues automatically - Continue on error")
    void generateMonthlyDuesAutomatically_ContinueOnError() {
        // Arrange
        ApartmentBuilding building2 = new ApartmentBuilding();
        building2.setId(2L);
        building2.setDefaultMonthlyFee(BigDecimal.valueOf(1500));
        
        when(apartmentBuildingRepository.findAll()).thenReturn(Arrays.asList(testBuilding, building2));
        when(apartmentBuildingRepository.findById(1L))
                .thenThrow(new RuntimeException("Database error"));
        when(apartmentBuildingRepository.findById(2L)).thenReturn(Optional.of(building2));
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(2L))
                .thenReturn(Collections.singletonList(testFlat2));
        when(monthlyDueRepository.save(any(MonthlyDue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        monthlyDueService.generateMonthlyDuesAutomatically();

        // Assert - Should continue with building 2 despite error with building 1
        verify(apartmentBuildingRepository).findById(1L);
        verify(apartmentBuildingRepository).findById(2L);
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(2L);
        verify(monthlyDueRepository).save(any()); // At least one save for building 2
    }

    // Tests for updateOverdueStatuses scheduled task
    @Test
    @DisplayName("Update overdue statuses - Success")
    void updateOverdueStatuses_Success() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(monthlyDueRepository.findByStatusAndDueDateBefore(MonthlyDue.DueStatus.UNPAID, today))
                .thenReturn(Arrays.asList(testDue1, testDue2));
        when(monthlyDueRepository.save(any(MonthlyDue.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        monthlyDueService.updateOverdueStatuses();

        // Assert
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.OVERDUE);
        assertThat(testDue2.getStatus()).isEqualTo(MonthlyDue.DueStatus.OVERDUE);
        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
    }

    @Test
    @DisplayName("Update overdue statuses - No overdue dues")
    void updateOverdueStatuses_NoOverdueDues() {
        // Arrange
        LocalDate today = LocalDate.now();
        when(monthlyDueRepository.findByStatusAndDueDateBefore(MonthlyDue.DueStatus.UNPAID, today))
                .thenReturn(Collections.emptyList());

        // Act
        monthlyDueService.updateOverdueStatuses();

        // Assert
        verify(monthlyDueRepository, never()).save(any());
    }

    // Tests for getDebtorsByBuilding method
    @Test
    @DisplayName("Get debtors by building - Success")
    void getDebtorsByBuilding_Success() {
        // Arrange
        List<Flat> expectedDebtors = Arrays.asList(testFlat1, testFlat2);
        when(monthlyDueRepository.findFlatsWithOverdueDues(1L)).thenReturn(expectedDebtors);

        // Act
        List<Flat> result = monthlyDueService.getDebtorsByBuilding(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(testFlat1, testFlat2);
        verify(monthlyDueRepository).findFlatsWithOverdueDues(1L);
    }

    // Tests for getDebtorDetailsForBuilding method
    @Test
    @DisplayName("Get debtor details for building - Success")
    void getDebtorDetailsForBuilding_Success() {
        // Arrange
        when(monthlyDueRepository.findFlatsWithOverdueDues(1L))
                .thenReturn(Arrays.asList(testFlat1, testFlat2));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L))
                .thenReturn(BigDecimal.valueOf(2000));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(2L))
                .thenReturn(BigDecimal.valueOf(1500));

        // Act
        Map<Flat, BigDecimal> result = monthlyDueService.getDebtorDetailsForBuilding(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(testFlat1)).isEqualTo(BigDecimal.valueOf(2000));
        assertThat(result.get(testFlat2)).isEqualTo(BigDecimal.valueOf(1500));
    }

    // Tests for calculateTotalDebt method
    @Test
    @DisplayName("Calculate total debt - Success")
    void calculateTotalDebt_Success() {
        // Arrange
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L))
                .thenReturn(BigDecimal.valueOf(3000));

        // Act
        BigDecimal result = monthlyDueService.calculateTotalDebt(1L);

        // Assert
        assertThat(result).isEqualTo(BigDecimal.valueOf(3000));
        verify(monthlyDueRepository).getTotalUnpaidDuesByFlat(1L);
    }

    @Test
    @DisplayName("Calculate total debt - Null returns zero")
    void calculateTotalDebt_NullReturnsZero() {
        // Arrange
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(null);

        // Act
        BigDecimal result = monthlyDueService.calculateTotalDebt(1L);

        // Assert
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    // Tests for getMonthlyDuesByFlat method
    @Test
    @DisplayName("Get monthly dues by flat - Success")
    void getMonthlyDuesByFlat_Success() {
        // Arrange
        List<MonthlyDue> expectedDues = Arrays.asList(testDue1, testDue2);
        when(monthlyDueRepository.findByFlatIdOrderByDueDateDesc(1L)).thenReturn(expectedDues);

        // Act
        List<MonthlyDue> result = monthlyDueService.getMonthlyDuesByFlat(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedDues);
        verify(monthlyDueRepository).findByFlatIdOrderByDueDateDesc(1L);
    }

    // Tests for getOverdueDuesByBuilding method
    @Test
    @DisplayName("Get overdue dues by building - Success")
    void getOverdueDuesByBuilding_Success() {
        // Arrange
        List<MonthlyDue> expectedDues = Arrays.asList(testDue1, testDue2);
        when(monthlyDueRepository.findOverdueDuesByBuilding(1L, MonthlyDue.DueStatus.OVERDUE))
                .thenReturn(expectedDues);

        // Act
        List<MonthlyDue> result = monthlyDueService.getOverdueDuesByBuilding(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedDues);
        verify(monthlyDueRepository).findOverdueDuesByBuilding(1L, MonthlyDue.DueStatus.OVERDUE);
    }

    // Tests for createMonthlyDue method
    @Test
    @DisplayName("Create monthly due - Success")
    void createMonthlyDue_Success() {
        // Arrange
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat1));
        when(monthlyDueRepository.save(any(MonthlyDue.class))).thenReturn(testDue1);

        // Act
        MonthlyDue result = monthlyDueService.createMonthlyDue(testDue1);

        // Assert
        assertThat(result).isEqualTo(testDue1);
        verify(flatRepository).findById(1L);
        verify(monthlyDueRepository).save(testDue1);
    }

    @Test
    @DisplayName("Create monthly due - Flat not found")
    void createMonthlyDue_FlatNotFound() {
        // Arrange
        when(flatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> monthlyDueService.createMonthlyDue(testDue1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Flat not found with ID: 1");

        // Verify
        verify(flatRepository).findById(1L);
        verify(monthlyDueRepository, never()).save(any());
    }

    // Tests for updateMonthlyDue method
    @Test
    @DisplayName("Update monthly due - Success")
    void updateMonthlyDue_Success() {
        // Arrange
        when(monthlyDueRepository.findById(1L)).thenReturn(Optional.of(testDue1));
        when(monthlyDueRepository.save(any(MonthlyDue.class))).thenReturn(testDue1);

        // Act
        MonthlyDue result = monthlyDueService.updateMonthlyDue(testDue1);

        // Assert
        assertThat(result).isNotNull();
        verify(monthlyDueRepository).findById(1L);
        verify(monthlyDueRepository).save(testDue1);
    }

    @Test
    @DisplayName("Update monthly due - Not found")
    void updateMonthlyDue_NotFound() {
        // Arrange
        testDue1.setId(999L);
        when(monthlyDueRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> monthlyDueService.updateMonthlyDue(testDue1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monthly due not found with ID: 999");

        // Verify
        verify(monthlyDueRepository).findById(999L);
        verify(monthlyDueRepository, never()).save(any());
    }

    // Tests for cancelMonthlyDue method
    @Test
    @DisplayName("Cancel monthly due - Success")
    void cancelMonthlyDue_Success() {
        // Arrange
        when(monthlyDueRepository.findById(1L)).thenReturn(Optional.of(testDue1));
        when(monthlyDueRepository.save(any(MonthlyDue.class))).thenReturn(testDue1);

        // Act
        monthlyDueService.cancelMonthlyDue(1L);

        // Assert
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.CANCELLED);
        verify(monthlyDueRepository).findById(1L);
        verify(monthlyDueRepository).save(testDue1);
    }

    @Test
    @DisplayName("Cancel monthly due - Not found")
    void cancelMonthlyDue_NotFound() {
        // Arrange
        when(monthlyDueRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> monthlyDueService.cancelMonthlyDue(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Monthly due not found with ID: 999");

        // Verify
        verify(monthlyDueRepository).findById(999L);
        verify(monthlyDueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancel monthly due - Cannot cancel paid due")
    void cancelMonthlyDue_CannotCancelPaidDue() {
        // Arrange
        testDue1.setStatus(MonthlyDue.DueStatus.PAID);
        when(monthlyDueRepository.findById(1L)).thenReturn(Optional.of(testDue1));

        // Act & Assert
        assertThatThrownBy(() -> monthlyDueService.cancelMonthlyDue(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel a paid monthly due");

        // Verify
        verify(monthlyDueRepository).findById(1L);
        verify(monthlyDueRepository, never()).save(any());
    }

    // Tests for getCollectionRate method
    @Test
    @DisplayName("Get collection rate - Success with paid dues")
    void getCollectionRate_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        // Create a mix of paid and unpaid dues
        MonthlyDue paidDue1 = new MonthlyDue();
        paidDue1.setStatus(MonthlyDue.DueStatus.PAID);
        
        MonthlyDue paidDue2 = new MonthlyDue();
        paidDue2.setStatus(MonthlyDue.DueStatus.PAID);
        
        MonthlyDue unpaidDue = new MonthlyDue();
        unpaidDue.setStatus(MonthlyDue.DueStatus.UNPAID);
        
        List<MonthlyDue> mixedDues = Arrays.asList(paidDue1, paidDue2, unpaidDue, testDue1, testDue2);
        
        when(monthlyDueRepository.findByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(mixedDues);

        // Act
        double result = monthlyDueService.getCollectionRate(1L, startDate, endDate);

        // Assert
        assertThat(result).isEqualTo(40.0); // 2 paid out of 5 total = 40%
        verify(monthlyDueRepository).findByBuildingAndDateRange(1L, startDate, endDate);
    }

    @Test
    @DisplayName("Get collection rate - No dues returns 100%")
    void getCollectionRate_NoDuesReturns100Percent() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        
        when(monthlyDueRepository.findByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(Collections.emptyList());

        // Act
        double result = monthlyDueService.getCollectionRate(1L, startDate, endDate);

        // Assert
        assertThat(result).isEqualTo(100.0); // No dues means 100% collection rate
    }
}