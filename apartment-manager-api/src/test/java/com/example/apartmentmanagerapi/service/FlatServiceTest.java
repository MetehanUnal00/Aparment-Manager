package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.Payment;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.FlatCreatedEvent;
import com.example.apartmentmanagerapi.exception.ResourceNotFoundException;
import com.example.apartmentmanagerapi.exception.DuplicateResourceException;
import com.example.apartmentmanagerapi.mapper.FlatMapper;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FlatService
 * Tests all service methods including creation, retrieval, update, deletion, and financial operations
 */
@ExtendWith(MockitoExtension.class)
class FlatServiceTest {

    @Mock
    private FlatRepository flatRepository;

    @Mock
    private ApartmentBuildingRepository apartmentBuildingRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private MonthlyDueService monthlyDueService;

    @Mock
    private FlatMapper flatMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private FlatService flatService;

    private FlatRequest testRequest;
    private Flat testFlat;
    private FlatResponse testResponse;
    private ApartmentBuilding testBuilding;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testRequest = new FlatRequest();
        testRequest.setFlatNumber("101");
        testRequest.setNumberOfRooms(3);
        testRequest.setAreaSqMeters(BigDecimal.valueOf(120.50));
        testRequest.setApartmentBuildingId(1L);
        testRequest.setIsActive(true);

        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test Street");

        testFlat = new Flat();
        testFlat.setId(1L);
        testFlat.setFlatNumber("101");
        testFlat.setNumberOfRooms(3);
        testFlat.setAreaSqMeters(BigDecimal.valueOf(120.50));
        testFlat.setApartmentBuilding(testBuilding);
        testFlat.setIsActive(true);

        testResponse = FlatResponse.builder()
                .id(1L)
                .flatNumber("101")
                .numberOfRooms(3)
                .areaSqMeters(BigDecimal.valueOf(120.50))
                .apartmentBuildingId(1L)
                .apartmentBuildingName("Test Building")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .currentBalance(BigDecimal.ZERO)
                .occupancyStatus(FlatResponse.OccupancyStatus.VACANT)
                .build();
    }

    // Tests for createFlat method
    @Test
    @DisplayName("Create flat - Success")
    void createFlat_Success() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndFlatNumber(1L, "101")).thenReturn(Optional.empty());
        when(flatMapper.toEntity(any(FlatRequest.class))).thenReturn(testFlat);
        when(flatRepository.save(any(Flat.class))).thenReturn(testFlat);
        when(flatMapper.toResponse(any(Flat.class))).thenReturn(testResponse);

        // Act
        FlatResponse result = flatService.createFlat(testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFlatNumber()).isEqualTo("101");

        // Verify interactions
        verify(apartmentBuildingRepository).findById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndFlatNumber(1L, "101");
        verify(flatMapper).toEntity(testRequest);
        verify(flatRepository).save(testFlat);
        verify(flatMapper).toResponse(testFlat);
        
        // Verify event was published
        ArgumentCaptor<FlatCreatedEvent> eventCaptor = ArgumentCaptor.forClass(FlatCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        FlatCreatedEvent event = eventCaptor.getValue();
        assertThat(event.getFlatId()).isEqualTo(1L);
        assertThat(event.getBuildingId()).isEqualTo(1L);
        assertThat(event.getFlatNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("Create flat - Building not found throws exception")
    void createFlat_BuildingNotFound_ThrowsException() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flatService.createFlat(testRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApartmentBuilding not found with id: 1");

        // Verify
        verify(apartmentBuildingRepository).findById(1L);
        verify(flatRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Create flat - Duplicate flat number throws exception")
    void createFlat_DuplicateFlatNumber_ThrowsException() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndFlatNumber(1L, "101")).thenReturn(Optional.of(testFlat));

        // Act & Assert
        assertThatThrownBy(() -> flatService.createFlat(testRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Flat already exists with flatNumber: 101");

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndFlatNumber(1L, "101");
        verify(flatRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // Tests for getAllFlatsByBuildingId method
    @Test
    @DisplayName("Get all flats by building ID - Success")
    void getAllFlatsByBuildingId_Success() {
        // Arrange
        Flat flat2 = new Flat();
        flat2.setId(2L);
        flat2.setFlatNumber("102");

        FlatResponse response2 = FlatResponse.builder()
                .id(2L)
                .flatNumber("102")
                .numberOfRooms(2)
                .areaSqMeters(BigDecimal.valueOf(80))
                .apartmentBuildingId(1L)
                .apartmentBuildingName("Test Building")
                .isActive(true)
                .currentBalance(BigDecimal.ZERO)
                .occupancyStatus(FlatResponse.OccupancyStatus.VACANT)
                .build();

        when(apartmentBuildingRepository.existsById(1L)).thenReturn(true);
        when(flatRepository.findByApartmentBuildingId(1L)).thenReturn(Arrays.asList(testFlat, flat2));
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);
        when(flatMapper.toResponse(flat2)).thenReturn(response2);

        // Act
        List<FlatResponse> results = flatService.getAllFlatsByBuildingId(1L);

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getFlatNumber()).isEqualTo("101");
        assertThat(results.get(1).getFlatNumber()).isEqualTo("102");

        // Verify
        verify(apartmentBuildingRepository).existsById(1L);
        verify(flatRepository).findByApartmentBuildingId(1L);
        verify(flatMapper, times(2)).toResponse(any(Flat.class));
    }

    @Test
    @DisplayName("Get all flats by building ID - Building not found throws exception")
    void getAllFlatsByBuildingId_BuildingNotFound_ThrowsException() {
        // Arrange
        when(apartmentBuildingRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> flatService.getAllFlatsByBuildingId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApartmentBuilding not found with id: 999");

        // Verify
        verify(apartmentBuildingRepository).existsById(999L);
        verify(flatRepository, never()).findByApartmentBuildingId(any());
    }

    // Tests for getFlatById method
    @Test
    @DisplayName("Get flat by ID - Success")
    void getFlatById_Success() {
        // Arrange
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);

        // Act
        FlatResponse result = flatService.getFlatById(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFlatNumber()).isEqualTo("101");

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(flatMapper).toResponse(testFlat);
    }

    @Test
    @DisplayName("Get flat by ID - Not found throws exception")
    void getFlatById_NotFound_ThrowsException() {
        // Arrange
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flatService.getFlatById(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flat not found with id: 999 in ApartmentBuilding: 1");

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 999L);
        verify(flatMapper, never()).toResponse(any());
    }

    // Tests for updateFlat method
    @Test
    @DisplayName("Update flat - Success with same flat number")
    void updateFlat_Success_SameFlatNumber() {
        // Arrange
        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatRepository.save(any(Flat.class))).thenReturn(testFlat);
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);

        // Act
        FlatResponse result = flatService.updateFlat(1L, 1L, testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        // Verify
        verify(apartmentBuildingRepository).findById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(flatRepository, never()).findByApartmentBuildingIdAndFlatNumber(any(), any()); // Not called for same flat number
        verify(flatMapper).updateEntityFromRequest(testRequest, testFlat);
        verify(flatRepository).save(testFlat);
    }

    @Test
    @DisplayName("Update flat - Success with new flat number")
    void updateFlat_Success_NewFlatNumber() {
        // Arrange
        FlatRequest updateRequest = new FlatRequest();
        updateRequest.setFlatNumber("103");
        updateRequest.setApartmentBuildingId(1L);
        updateRequest.setNumberOfRooms(3);
        updateRequest.setAreaSqMeters(BigDecimal.valueOf(120));

        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatRepository.findByApartmentBuildingIdAndFlatNumber(1L, "103")).thenReturn(Optional.empty());
        when(flatRepository.save(any(Flat.class))).thenReturn(testFlat);
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);

        // Act
        FlatResponse result = flatService.updateFlat(1L, 1L, updateRequest);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndFlatNumber(1L, "103");
        verify(flatMapper).updateEntityFromRequest(updateRequest, testFlat);
        verify(flatRepository).save(testFlat);
    }

    @Test
    @DisplayName("Update flat - Duplicate new flat number throws exception")
    void updateFlat_DuplicateNewFlatNumber_ThrowsException() {
        // Arrange
        FlatRequest updateRequest = new FlatRequest();
        updateRequest.setFlatNumber("102"); // Different from current flat number
        updateRequest.setApartmentBuildingId(1L);

        Flat existingFlat = new Flat();
        existingFlat.setFlatNumber("102");

        when(apartmentBuildingRepository.findById(1L)).thenReturn(Optional.of(testBuilding));
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatRepository.findByApartmentBuildingIdAndFlatNumber(1L, "102")).thenReturn(Optional.of(existingFlat));

        // Act & Assert
        assertThatThrownBy(() -> flatService.updateFlat(1L, 1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Flat already exists with flatNumber: 102");

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndFlatNumber(1L, "102");
        verify(flatRepository, never()).save(any());
    }

    // Tests for deleteFlat method
    @Test
    @DisplayName("Delete flat - Success with zero balance")
    void deleteFlat_Success_ZeroBalance() {
        // Arrange
        when(flatRepository.existsById(1L)).thenReturn(true);
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(paymentService.calculateOutstandingBalance(1L)).thenReturn(BigDecimal.ZERO);

        // Act
        flatService.deleteFlat(1L, 1L);

        // Assert & Verify
        verify(flatRepository).existsById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(paymentService).calculateOutstandingBalance(1L);
        verify(flatRepository).delete(testFlat);
    }

    @Test
    @DisplayName("Delete flat - Success with outstanding balance (warning logged)")
    void deleteFlat_Success_WithOutstandingBalance() {
        // Arrange
        when(flatRepository.existsById(1L)).thenReturn(true);
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(paymentService.calculateOutstandingBalance(1L)).thenReturn(BigDecimal.valueOf(500));

        // Act
        flatService.deleteFlat(1L, 1L);

        // Assert & Verify
        verify(flatRepository).existsById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(paymentService).calculateOutstandingBalance(1L);
        verify(flatRepository).delete(testFlat); // Still deletes despite balance
    }

    @Test
    @DisplayName("Delete flat - Not found throws exception")
    void deleteFlat_NotFound_ThrowsException() {
        // Arrange
        when(flatRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> flatService.deleteFlat(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flat not found with id: 999");

        // Verify
        verify(flatRepository).existsById(999L);
        verify(flatRepository, never()).delete(any());
    }

    // Tests for getFlatWithFinancialInfo method
    @Test
    @DisplayName("Get flat with financial info - Success")
    void getFlatWithFinancialInfo_Success() {
        // Arrange
        List<Payment> mockPayments = Arrays.asList(new Payment());
        List<MonthlyDue> mockDues = Arrays.asList(new MonthlyDue());

        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);
        when(paymentService.calculateOutstandingBalance(1L)).thenReturn(BigDecimal.valueOf(1000));
        when(monthlyDueService.calculateTotalDebt(1L)).thenReturn(BigDecimal.valueOf(1500));
        when(paymentService.getPaymentsByFlat(1L)).thenReturn(mockPayments);
        when(monthlyDueService.getMonthlyDuesByFlat(1L)).thenReturn(mockDues);

        // Act
        Map<String, Object> result = flatService.getFlatWithFinancialInfo(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).containsKey("flat");
        assertThat(result).containsKey("currentBalance");
        assertThat(result).containsKey("totalDebt");
        assertThat(result).containsKey("recentPayments");
        assertThat(result).containsKey("monthlyDues");
        assertThat(result.get("currentBalance")).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(result.get("totalDebt")).isEqualTo(BigDecimal.valueOf(1500));

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(paymentService).calculateOutstandingBalance(1L);
        verify(monthlyDueService).calculateTotalDebt(1L);
    }

    // Tests for getActiveFlatsByBuildingId method
    @Test
    @DisplayName("Get active flats by building ID - Success")
    void getActiveFlatsByBuildingId_Success() {
        // Arrange
        when(apartmentBuildingRepository.existsById(1L)).thenReturn(true);
        when(flatRepository.findByApartmentBuildingIdAndIsActiveTrue(1L)).thenReturn(Arrays.asList(testFlat));
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);

        // Act
        List<FlatResponse> results = flatService.getActiveFlatsByBuildingId(1L);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsActive()).isTrue();

        // Verify
        verify(apartmentBuildingRepository).existsById(1L);
        verify(flatRepository).findByApartmentBuildingIdAndIsActiveTrue(1L);
        verify(flatMapper).toResponse(testFlat);
    }

    // Tests for updateTenantInfo method - REMOVED
    // Tenant info is now managed through contracts, not flats
    // @Test
    // @DisplayName("Update tenant info - Success")
    // void updateTenantInfo_Success() {
    //     This test has been removed because tenant information
    //     is now managed through the Contract entity
    // }

    // Tests for deactivateFlat method
    @Test
    @DisplayName("Deactivate flat - Success")
    void deactivateFlat_Success() {
        // Arrange
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 1L)).thenReturn(Optional.of(testFlat));
        when(flatRepository.save(any(Flat.class))).thenReturn(testFlat);
        when(flatMapper.toResponse(testFlat)).thenReturn(testResponse);

        // Act
        FlatResponse result = flatService.deactivateFlat(1L, 1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(testFlat.getIsActive()).isFalse(); // Verify flat was deactivated

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 1L);
        verify(flatRepository).save(testFlat);
    }

    @Test
    @DisplayName("Deactivate flat - Not found throws exception")
    void deactivateFlat_NotFound_ThrowsException() {
        // Arrange
        when(flatRepository.findByApartmentBuildingIdAndId(1L, 999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> flatService.deactivateFlat(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Flat not found with id: 999 in ApartmentBuilding: 1");

        // Verify
        verify(flatRepository).findByApartmentBuildingIdAndId(1L, 999L);
        verify(flatRepository, never()).save(any());
    }
}