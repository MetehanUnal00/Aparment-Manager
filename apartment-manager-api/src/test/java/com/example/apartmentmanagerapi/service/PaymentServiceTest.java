package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.Payment;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.PaymentRecordedEvent;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.PaymentRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService
 * Tests payment creation, allocation, balance calculation, and transaction management
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FlatRepository flatRepository;

    @Mock
    private MonthlyDueRepository monthlyDueRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private Flat testFlat;
    private ApartmentBuilding testBuilding;
    private MonthlyDue testDue1;
    private MonthlyDue testDue2;

    @BeforeEach
    void setUp() {
        // Initialize test building
        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test Street");

        // Initialize test flat
        testFlat = new Flat();
        testFlat.setId(1L);
        testFlat.setFlatNumber("101");
        testFlat.setApartmentBuilding(testBuilding);
        testFlat.setTenantName("John Doe");
        testFlat.setTenantEmail("john@example.com");

        // Initialize test payment
        testPayment = new Payment();
        testPayment.setFlat(testFlat);
        testPayment.setAmount(BigDecimal.valueOf(1500));
        testPayment.setPaymentDate(LocalDateTime.now());
        testPayment.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
        testPayment.setDescription("Monthly rent payment");
        testPayment.setReceiptNumber("RCP001");

        // Initialize test monthly dues
        testDue1 = new MonthlyDue();
        testDue1.setId(1L);
        testDue1.setFlat(testFlat);
        testDue1.setDueAmount(BigDecimal.valueOf(1000));
        testDue1.setPaidAmount(BigDecimal.ZERO);
        testDue1.setStatus(MonthlyDue.DueStatus.UNPAID);
        testDue1.setDueDate(LocalDate.now().minusMonths(1));

        testDue2 = new MonthlyDue();
        testDue2.setId(2L);
        testDue2.setFlat(testFlat);
        testDue2.setDueAmount(BigDecimal.valueOf(1000));
        testDue2.setPaidAmount(BigDecimal.ZERO);
        testDue2.setStatus(MonthlyDue.DueStatus.UNPAID);
        testDue2.setDueDate(LocalDate.now());
    }

    // Tests for createPayment method
    @Test
    @DisplayName("Create payment - Success with full allocation")
    void createPayment_Success_FullAllocation() {
        // Arrange
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(BigDecimal.valueOf(2000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(monthlyDueRepository.findUnpaidDuesByFlatOrderByDueDate(1L))
                .thenReturn(Arrays.asList(testDue1, testDue2));

        // Act
        Payment result = paymentService.createPayment(testPayment);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(testPayment);

        // Verify interactions
        verify(flatRepository).findById(1L);
        verify(monthlyDueRepository).getTotalUnpaidDuesByFlat(1L);
        verify(paymentRepository).save(testPayment);
        verify(monthlyDueRepository).findUnpaidDuesByFlatOrderByDueDate(1L);
        
        // Verify dues were updated
        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
        
        // Verify first due was fully paid
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.PAID);
        assertThat(testDue1.getPaidAmount()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(testDue1.getPaymentDate()).isEqualTo(testPayment.getPaymentDate());
        
        // Verify second due was partially paid
        assertThat(testDue2.getStatus()).isEqualTo(MonthlyDue.DueStatus.PARTIALLY_PAID);
        assertThat(testDue2.getPaidAmount()).isEqualTo(BigDecimal.valueOf(500));

        // Verify event was published
        ArgumentCaptor<PaymentRecordedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRecordedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        PaymentRecordedEvent event = eventCaptor.getValue();
        assertThat(event.getFlatId()).isEqualTo(1L);
        assertThat(event.getBuildingId()).isEqualTo(1L);
        assertThat(event.getAmount()).isEqualTo(BigDecimal.valueOf(1500));
    }

    @Test
    @DisplayName("Create payment - Flat not found throws exception")
    void createPayment_FlatNotFound_ThrowsException() {
        // Arrange
        when(flatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(testPayment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Flat not found with ID: 1");

        // Verify
        verify(flatRepository).findById(1L);
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Create payment - Amount exceeds balance throws exception")
    void createPayment_ExceedsBalance_ThrowsException() {
        // Arrange
        testPayment.setAmount(BigDecimal.valueOf(3000));
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(BigDecimal.valueOf(2000));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.createPayment(testPayment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment amount exceeds outstanding balance");

        // Verify
        verify(flatRepository).findById(1L);
        verify(monthlyDueRepository).getTotalUnpaidDuesByFlat(1L);
        verify(paymentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Create payment - Overpayment warning logged")
    void createPayment_Overpayment_WarningLogged() {
        // Arrange
        testPayment.setAmount(BigDecimal.valueOf(500)); // Less than total dues
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(BigDecimal.valueOf(2000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(monthlyDueRepository.findUnpaidDuesByFlatOrderByDueDate(1L))
                .thenReturn(Collections.singletonList(testDue1));

        // Act
        Payment result = paymentService.createPayment(testPayment);

        // Assert
        assertThat(result).isNotNull();
        
        // Verify partial payment
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.PARTIALLY_PAID);
        assertThat(testDue1.getPaidAmount()).isEqualTo(BigDecimal.valueOf(500));
        
        verify(monthlyDueRepository).save(testDue1);
    }

    // Tests for getPaymentsByFlat method
    @Test
    @DisplayName("Get payments by flat - Success")
    void getPaymentsByFlat_Success() {
        // Arrange
        List<Payment> expectedPayments = Arrays.asList(testPayment);
        when(paymentRepository.findByFlatIdOrderByPaymentDateDesc(1L)).thenReturn(expectedPayments);

        // Act
        List<Payment> result = paymentService.getPaymentsByFlat(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedPayments);

        // Verify
        verify(paymentRepository).findByFlatIdOrderByPaymentDateDesc(1L);
    }

    // Tests for getPaymentsByBuildingAndDateRange method
    @Test
    @DisplayName("Get payments by building and date range - Success")
    void getPaymentsByBuildingAndDateRange_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        List<Payment> expectedPayments = Arrays.asList(testPayment);
        when(paymentRepository.findByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(expectedPayments);

        // Act
        List<Payment> result = paymentService.getPaymentsByBuildingAndDateRange(1L, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result).isEqualTo(expectedPayments);

        // Verify
        verify(paymentRepository).findByBuildingAndDateRange(1L, startDate, endDate);
    }

    // Tests for getTotalPaymentsByBuildingAndDateRange method
    @Test
    @DisplayName("Get total payments by building and date range - Success")
    void getTotalPaymentsByBuildingAndDateRange_Success() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        BigDecimal expectedTotal = BigDecimal.valueOf(5000);
        when(paymentRepository.getTotalPaymentsByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(expectedTotal);

        // Act
        BigDecimal result = paymentService.getTotalPaymentsByBuildingAndDateRange(1L, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedTotal);

        // Verify
        verify(paymentRepository).getTotalPaymentsByBuildingAndDateRange(1L, startDate, endDate);
    }

    @Test
    @DisplayName("Get total payments by building and date range - Null returns zero")
    void getTotalPaymentsByBuildingAndDateRange_NullReturnsZero() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusMonths(1);
        LocalDate endDate = LocalDate.now();
        when(paymentRepository.getTotalPaymentsByBuildingAndDateRange(1L, startDate, endDate))
                .thenReturn(null);

        // Act
        BigDecimal result = paymentService.getTotalPaymentsByBuildingAndDateRange(1L, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    // Tests for calculateOutstandingBalance method
    @Test
    @DisplayName("Calculate outstanding balance - Success")
    void calculateOutstandingBalance_Success() {
        // Arrange
        BigDecimal expectedBalance = BigDecimal.valueOf(2000);
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(expectedBalance);

        // Act
        BigDecimal result = paymentService.calculateOutstandingBalance(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedBalance);

        // Verify
        verify(monthlyDueRepository).getTotalUnpaidDuesByFlat(1L);
    }

    @Test
    @DisplayName("Calculate outstanding balance - Null returns zero")
    void calculateOutstandingBalance_NullReturnsZero() {
        // Arrange
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(null);

        // Act
        BigDecimal result = paymentService.calculateOutstandingBalance(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    // Tests for updatePayment method
    @Test
    @DisplayName("Update payment - Success")
    void updatePayment_Success() {
        // Arrange
        testPayment.setId(1L);
        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setPaymentMethod(Payment.PaymentMethod.CASH);
        existingPayment.setDescription("Old description");
        existingPayment.setReceiptNumber("OLD001");

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(existingPayment);

        // Act
        Payment result = paymentService.updatePayment(testPayment);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentMethod()).isEqualTo(Payment.PaymentMethod.BANK_TRANSFER);
        assertThat(result.getDescription()).isEqualTo("Monthly rent payment");
        assertThat(result.getReceiptNumber()).isEqualTo("RCP001");

        // Verify
        verify(paymentRepository).findById(1L);
        verify(paymentRepository).save(existingPayment);
    }

    @Test
    @DisplayName("Update payment - Not found throws exception")
    void updatePayment_NotFound_ThrowsException() {
        // Arrange
        testPayment.setId(999L);
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.updatePayment(testPayment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found with ID: 999");

        // Verify
        verify(paymentRepository).findById(999L);
        verify(paymentRepository, never()).save(any());
    }

    // Tests for deletePayment method
    @Test
    @DisplayName("Delete payment - Success with allocation reversal")
    void deletePayment_Success_WithAllocationReversal() {
        // Arrange
        testPayment.setId(1L);
        testDue1.setStatus(MonthlyDue.DueStatus.PAID);
        testDue1.setPaidAmount(BigDecimal.valueOf(1000));
        testDue1.setPaymentDate(testPayment.getPaymentDate());

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(monthlyDueRepository.findByFlatIdAndPaymentDate(1L, testPayment.getPaymentDate()))
                .thenReturn(Collections.singletonList(testDue1));

        // Act
        paymentService.deletePayment(1L);

        // Assert
        // Verify allocation was reversed
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.UNPAID);
        assertThat(testDue1.getPaidAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(testDue1.getPaymentDate()).isNull();

        // Verify interactions
        verify(paymentRepository).findById(1L);
        verify(monthlyDueRepository).findByFlatIdAndPaymentDate(1L, testPayment.getPaymentDate());
        verify(monthlyDueRepository).save(testDue1);
        verify(paymentRepository).delete(testPayment);
    }

    @Test
    @DisplayName("Delete payment - Not found throws exception")
    void deletePayment_NotFound_ThrowsException() {
        // Arrange
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.deletePayment(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found with ID: 999");

        // Verify
        verify(paymentRepository).findById(999L);
        verify(paymentRepository, never()).delete(any());
    }

    // Tests for allocation logic
    @Test
    @DisplayName("Allocate payment - Partial payment across multiple dues")
    void allocatePayment_PartialAcrossMultipleDues() {
        // Arrange
        testPayment.setAmount(BigDecimal.valueOf(1200)); // Partial payment for 2 dues
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(BigDecimal.valueOf(2000));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(monthlyDueRepository.findUnpaidDuesByFlatOrderByDueDate(1L))
                .thenReturn(Arrays.asList(testDue1, testDue2));

        // Act
        paymentService.createPayment(testPayment);

        // Assert
        // First due should be fully paid
        assertThat(testDue1.getStatus()).isEqualTo(MonthlyDue.DueStatus.PAID);
        assertThat(testDue1.getPaidAmount()).isEqualTo(BigDecimal.valueOf(1000));
        
        // Second due should be partially paid
        assertThat(testDue2.getStatus()).isEqualTo(MonthlyDue.DueStatus.PARTIALLY_PAID);
        assertThat(testDue2.getPaidAmount()).isEqualTo(BigDecimal.valueOf(200));

        verify(monthlyDueRepository, times(2)).save(any(MonthlyDue.class));
    }

    @Test
    @DisplayName("Allocate payment - No unpaid dues")
    void allocatePayment_NoUnpaidDues() {
        // Arrange
        testPayment.setAmount(BigDecimal.valueOf(100));
        when(flatRepository.findById(1L)).thenReturn(Optional.of(testFlat));
        when(monthlyDueRepository.getTotalUnpaidDuesByFlat(1L)).thenReturn(BigDecimal.valueOf(100));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(monthlyDueRepository.findUnpaidDuesByFlatOrderByDueDate(1L))
                .thenReturn(Collections.emptyList());

        // Act
        paymentService.createPayment(testPayment);

        // Assert
        verify(monthlyDueRepository).findUnpaidDuesByFlatOrderByDueDate(1L);
        verify(monthlyDueRepository, never()).save(any(MonthlyDue.class));
    }
}