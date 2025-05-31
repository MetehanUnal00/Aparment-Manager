package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;
import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.exception.DuplicateResourceException;
import com.example.apartmentmanagerapi.exception.ResourceNotFoundException;
import com.example.apartmentmanagerapi.mapper.ApartmentBuildingMapper;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApartmentBuildingService
 * Tests all service methods with various scenarios including positive and negative cases
 */
@ExtendWith(MockitoExtension.class)
class ApartmentBuildingServiceTest {

    @Mock
    private ApartmentBuildingRepository apartmentBuildingRepository;

    @Mock
    private ApartmentBuildingMapper apartmentBuildingMapper;

    @InjectMocks
    private ApartmentBuildingService apartmentBuildingService;

    private ApartmentBuildingRequest testRequest;
    private ApartmentBuilding testBuilding;
    private ApartmentBuildingResponse testResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testRequest = new ApartmentBuildingRequest();
        testRequest.setName("Test Building");
        testRequest.setAddress("123 Test Street");

        testBuilding = new ApartmentBuilding();
        testBuilding.setId(1L);
        testBuilding.setName("Test Building");
        testBuilding.setAddress("123 Test Street");

        testResponse = new ApartmentBuildingResponse(
                1L,
                "Test Building",
                "123 Test Street",
                null,  // createdAt - will be set by mapper
                null   // updatedAt - will be set by mapper
        );
    }

    // Tests for createApartmentBuilding method
    @Test
    @DisplayName("Create apartment building - Success")
    void createApartmentBuilding_Success() {
        // Arrange
        when(apartmentBuildingRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(apartmentBuildingMapper.toEntity(any(ApartmentBuildingRequest.class))).thenReturn(testBuilding);
        when(apartmentBuildingRepository.save(any(ApartmentBuilding.class))).thenReturn(testBuilding);
        when(apartmentBuildingMapper.toResponse(any(ApartmentBuilding.class))).thenReturn(testResponse);

        // Act
        ApartmentBuildingResponse result = apartmentBuildingService.createApartmentBuilding(testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Building");

        // Verify interactions
        verify(apartmentBuildingRepository).findByName("Test Building");
        verify(apartmentBuildingMapper).toEntity(testRequest);
        verify(apartmentBuildingRepository).save(testBuilding);
        verify(apartmentBuildingMapper).toResponse(testBuilding);
    }

    @Test
    @DisplayName("Create apartment building - Duplicate name throws exception")
    void createApartmentBuilding_DuplicateName_ThrowsException() {
        // Arrange
        when(apartmentBuildingRepository.findByName(anyString())).thenReturn(Optional.of(testBuilding));

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.createApartmentBuilding(testRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ApartmentBuilding already exists with name: Test Building");

        // Verify only repository check was called
        verify(apartmentBuildingRepository).findByName("Test Building");
        verify(apartmentBuildingRepository, never()).save(any());
        verify(apartmentBuildingMapper, never()).toEntity(any());
    }

    // Tests for getAllApartmentBuildings method
    @Test
    @DisplayName("Get all apartment buildings - Success")
    void getAllApartmentBuildings_Success() {
        // Arrange
        ApartmentBuilding building2 = new ApartmentBuilding();
        building2.setId(2L);
        building2.setName("Building 2");

        ApartmentBuildingResponse response2 = new ApartmentBuildingResponse(
                2L,
                "Building 2",
                null,  // address
                null,  // createdAt
                null   // updatedAt
        );

        List<ApartmentBuilding> buildings = Arrays.asList(testBuilding, building2);
        when(apartmentBuildingRepository.findAll()).thenReturn(buildings);
        when(apartmentBuildingMapper.toResponse(testBuilding)).thenReturn(testResponse);
        when(apartmentBuildingMapper.toResponse(building2)).thenReturn(response2);

        // Act
        List<ApartmentBuildingResponse> results = apartmentBuildingService.getAllApartmentBuildings();

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Test Building");
        assertThat(results.get(1).getName()).isEqualTo("Building 2");

        // Verify
        verify(apartmentBuildingRepository).findAll();
        verify(apartmentBuildingMapper, times(2)).toResponse(any(ApartmentBuilding.class));
    }

    @Test
    @DisplayName("Get all apartment buildings - Empty list")
    void getAllApartmentBuildings_EmptyList() {
        // Arrange
        when(apartmentBuildingRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<ApartmentBuildingResponse> results = apartmentBuildingService.getAllApartmentBuildings();

        // Assert
        assertThat(results).isEmpty();

        // Verify
        verify(apartmentBuildingRepository).findAll();
        verify(apartmentBuildingMapper, never()).toResponse(any());
    }

    // Tests for getApartmentBuildingById method
    @Test
    @DisplayName("Get apartment building by ID - Success")
    void getApartmentBuildingById_Success() {
        // Arrange
        Long buildingId = 1L;
        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.of(testBuilding));
        when(apartmentBuildingMapper.toResponse(testBuilding)).thenReturn(testResponse);

        // Act
        ApartmentBuildingResponse result = apartmentBuildingService.getApartmentBuildingById(buildingId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(buildingId);
        assertThat(result.getName()).isEqualTo("Test Building");

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingMapper).toResponse(testBuilding);
    }

    @Test
    @DisplayName("Get apartment building by ID - Not found throws exception")
    void getApartmentBuildingById_NotFound_ThrowsException() {
        // Arrange
        Long buildingId = 999L;
        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.getApartmentBuildingById(buildingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApartmentBuilding not found with id: 999");

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingMapper, never()).toResponse(any());
    }

    // Tests for updateApartmentBuilding method
    @Test
    @DisplayName("Update apartment building - Success with same name")
    void updateApartmentBuilding_Success_SameName() {
        // Arrange
        Long buildingId = 1L;
        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.of(testBuilding));
        when(apartmentBuildingRepository.save(any(ApartmentBuilding.class))).thenReturn(testBuilding);
        when(apartmentBuildingMapper.toResponse(testBuilding)).thenReturn(testResponse);

        // Act
        ApartmentBuildingResponse result = apartmentBuildingService.updateApartmentBuilding(buildingId, testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(buildingId);

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingRepository, never()).findByName(anyString()); // Name didn't change
        verify(apartmentBuildingMapper).updateEntityFromRequest(testRequest, testBuilding);
        verify(apartmentBuildingRepository).save(testBuilding);
        verify(apartmentBuildingMapper).toResponse(testBuilding);
    }

    @Test
    @DisplayName("Update apartment building - Success with new name")
    void updateApartmentBuilding_Success_NewName() {
        // Arrange
        Long buildingId = 1L;
        ApartmentBuildingRequest updateRequest = new ApartmentBuildingRequest();
        updateRequest.setName("New Building Name");
        updateRequest.setAddress("456 New Street");

        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.of(testBuilding));
        when(apartmentBuildingRepository.findByName("New Building Name")).thenReturn(Optional.empty());
        when(apartmentBuildingRepository.save(any(ApartmentBuilding.class))).thenReturn(testBuilding);
        when(apartmentBuildingMapper.toResponse(testBuilding)).thenReturn(testResponse);

        // Act
        ApartmentBuildingResponse result = apartmentBuildingService.updateApartmentBuilding(buildingId, updateRequest);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingRepository).findByName("New Building Name");
        verify(apartmentBuildingMapper).updateEntityFromRequest(updateRequest, testBuilding);
        verify(apartmentBuildingRepository).save(testBuilding);
    }

    @Test
    @DisplayName("Update apartment building - Building not found throws exception")
    void updateApartmentBuilding_NotFound_ThrowsException() {
        // Arrange
        Long buildingId = 999L;
        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.updateApartmentBuilding(buildingId, testRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApartmentBuilding not found with id: 999");

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update apartment building - Duplicate new name throws exception")
    void updateApartmentBuilding_DuplicateNewName_ThrowsException() {
        // Arrange
        Long buildingId = 1L;
        ApartmentBuildingRequest updateRequest = new ApartmentBuildingRequest();
        updateRequest.setName("Existing Building");
        updateRequest.setAddress("456 New Street");

        ApartmentBuilding existingBuilding = new ApartmentBuilding();
        existingBuilding.setId(2L);
        existingBuilding.setName("Existing Building");

        when(apartmentBuildingRepository.findById(buildingId)).thenReturn(Optional.of(testBuilding));
        when(apartmentBuildingRepository.findByName("Existing Building")).thenReturn(Optional.of(existingBuilding));

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.updateApartmentBuilding(buildingId, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ApartmentBuilding already exists with name: Existing Building");

        // Verify
        verify(apartmentBuildingRepository).findById(buildingId);
        verify(apartmentBuildingRepository).findByName("Existing Building");
        verify(apartmentBuildingRepository, never()).save(any());
    }

    // Tests for deleteApartmentBuilding method
    @Test
    @DisplayName("Delete apartment building - Success")
    void deleteApartmentBuilding_Success() {
        // Arrange
        Long buildingId = 1L;
        when(apartmentBuildingRepository.existsById(buildingId)).thenReturn(true);

        // Act
        apartmentBuildingService.deleteApartmentBuilding(buildingId);

        // Assert & Verify
        verify(apartmentBuildingRepository).existsById(buildingId);
        verify(apartmentBuildingRepository).deleteById(buildingId);
    }

    @Test
    @DisplayName("Delete apartment building - Not found throws exception")
    void deleteApartmentBuilding_NotFound_ThrowsException() {
        // Arrange
        Long buildingId = 999L;
        when(apartmentBuildingRepository.existsById(buildingId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.deleteApartmentBuilding(buildingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ApartmentBuilding not found with id: 999");

        // Verify
        verify(apartmentBuildingRepository).existsById(buildingId);
        verify(apartmentBuildingRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Delete apartment building - Repository exception propagates")
    void deleteApartmentBuilding_RepositoryException_Propagates() {
        // Arrange
        Long buildingId = 1L;
        when(apartmentBuildingRepository.existsById(buildingId)).thenReturn(true);
        doThrow(new RuntimeException("Database error")).when(apartmentBuildingRepository).deleteById(buildingId);

        // Act & Assert
        assertThatThrownBy(() -> apartmentBuildingService.deleteApartmentBuilding(buildingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        // Verify
        verify(apartmentBuildingRepository).existsById(buildingId);
        verify(apartmentBuildingRepository).deleteById(buildingId);
    }
}