package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.mapper.FlatMapper;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlatService implements IFlatService {

    private final FlatRepository flatRepository;
    private final ApartmentBuildingRepository apartmentBuildingRepository;
    private final PaymentService paymentService;
    private final MonthlyDueService monthlyDueService;
    private final FlatMapper flatMapper;

    @Transactional
    public FlatResponse createFlat(FlatRequest request) {
        // Find the apartment building that this flat will belong to
        ApartmentBuilding building = apartmentBuildingRepository.findById(request.getApartmentBuildingId())
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + request.getApartmentBuildingId()));

        // Check if flat number already exists in this building
        if (flatRepository.findByApartmentBuildingIdAndFlatNumber(request.getApartmentBuildingId(), request.getFlatNumber()).isPresent()) {
            throw new RuntimeException("Error: Flat number " + request.getFlatNumber() + " already exists in this building.");
        }

        // Map the request to entity using MapStruct
        Flat flat = flatMapper.toEntity(request);
        
        // Set the apartment building relationship (not handled by mapper)
        flat.setApartmentBuilding(building);
        
        // Save the flat entity
        Flat savedFlat = flatRepository.save(flat);
        
        // Map entity to response and return
        return flatMapper.toResponse(savedFlat);
    }

    @Transactional(readOnly = true)
    public List<FlatResponse> getAllFlatsByBuildingId(Long buildingId) {
        // Verify apartment building exists
        if (!apartmentBuildingRepository.existsById(buildingId)) {
            throw new RuntimeException("Error: Apartment building not found with id: " + buildingId);
        }
        
        // Find all flats and map to response DTOs
        return flatRepository.findByApartmentBuildingId(buildingId).stream()
                .map(flatMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlatResponse getFlatById(Long buildingId, Long flatId) {
        // Find flat and map to response
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        return flatMapper.toResponse(flat);
    }

    @Transactional
    public FlatResponse updateFlat(Long buildingId, Long flatId, FlatRequest request) {
        // Verify apartment building exists
        ApartmentBuilding building = apartmentBuildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + buildingId));

        // Find the flat to update
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));

        // Check if flat number is being changed and if the new number is already taken in the same building
        if (!flat.getFlatNumber().equals(request.getFlatNumber()) &&
            flatRepository.findByApartmentBuildingIdAndFlatNumber(buildingId, request.getFlatNumber()).isPresent()) {
            throw new RuntimeException("Error: New flat number " + request.getFlatNumber() + " already exists in this building.");
        }

        // Update the flat entity from request using MapStruct
        flatMapper.updateEntityFromRequest(request, flat);
        
        // Handle special case: if building is being changed (complex operation)
        // Note: Changing apartmentBuildingId for an existing flat might be complex or disallowed.
        // For now, we assume the flat stays within the same building or this request.getApartmentBuildingId() matches the current buildingId.
        // If you need to move a flat to a different building, that's a more complex operation.
        if (!buildingId.equals(request.getApartmentBuildingId())) {
            ApartmentBuilding newBuilding = apartmentBuildingRepository.findById(request.getApartmentBuildingId())
                .orElseThrow(() -> new RuntimeException("Error: New Apartment building not found with id: " + request.getApartmentBuildingId()));
            flat.setApartmentBuilding(newBuilding);
        }

        // Save and return the updated flat
        Flat updatedFlat = flatRepository.save(flat);
        return flatMapper.toResponse(updatedFlat);
    }

    @Transactional
    public void deleteFlat(Long buildingId, Long flatId) {
        if (!flatRepository.existsById(flatId)) {
             throw new RuntimeException("Error: Flat not found with id: " + flatId);
        }
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        
        // Check for outstanding balance before deletion
        BigDecimal balance = paymentService.calculateOutstandingBalance(flatId);
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Deleting flat {} with outstanding balance: {}", flatId, balance);
        }
        
        // Consider implications: what happens to tenants in this flat?
        // For soft delete, consider setting isActive to false instead
        flatRepository.delete(flat);
    }
    
    /**
     * Get flat with additional financial information
     * Includes current balance and recent payment history
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFlatWithFinancialInfo(Long buildingId, Long flatId) {
        // Find the flat
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        
        // Build comprehensive financial information map
        Map<String, Object> flatInfo = new HashMap<>();
        flatInfo.put("flat", flatMapper.toResponse(flat));
        flatInfo.put("currentBalance", paymentService.calculateOutstandingBalance(flatId));
        flatInfo.put("totalDebt", monthlyDueService.calculateTotalDebt(flatId));
        flatInfo.put("recentPayments", paymentService.getPaymentsByFlat(flatId).stream()
                .limit(5)
                .collect(Collectors.toList()));
        flatInfo.put("monthlyDues", monthlyDueService.getMonthlyDuesByFlat(flatId).stream()
                .limit(5)
                .collect(Collectors.toList()));
        
        return flatInfo;
    }
    
    /**
     * Get all active flats in a building
     * Only returns flats where isActive = true
     */
    @Transactional(readOnly = true)
    public List<FlatResponse> getActiveFlatsByBuildingId(Long buildingId) {
        // Verify apartment building exists
        if (!apartmentBuildingRepository.existsById(buildingId)) {
            throw new RuntimeException("Error: Apartment building not found with id: " + buildingId);
        }
        
        // Find active flats and map to response DTOs
        return flatRepository.findByApartmentBuildingIdAndIsActiveTrue(buildingId).stream()
                .map(flatMapper::toResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update tenant information for a flat
     * Used when a new tenant moves in or existing tenant info changes
     */
    @Transactional
    public FlatResponse updateTenantInfo(Long buildingId, Long flatId, FlatRequest request) {
        // Find the flat to update
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        
        // Update only tenant-related fields
        flat.setTenantName(request.getTenantName());
        flat.setTenantContact(request.getTenantContact());
        flat.setTenantEmail(request.getTenantEmail());
        flat.setMonthlyRent(request.getMonthlyRent());
        flat.setSecurityDeposit(request.getSecurityDeposit());
        flat.setTenantMoveInDate(request.getTenantMoveInDate() != null ? request.getTenantMoveInDate() : LocalDateTime.now());
        
        log.info("Updated tenant information for flat {}: {}", flatId, request.getTenantName());
        
        // Save and return updated flat
        Flat updatedFlat = flatRepository.save(flat);
        return flatMapper.toResponse(updatedFlat);
    }
    
    /**
     * Mark a flat as inactive
     * Inactive flats won't be included in monthly due generation
     */
    @Transactional
    public FlatResponse deactivateFlat(Long buildingId, Long flatId) {
        // Find the flat to deactivate
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        
        // Mark as inactive
        flat.setIsActive(false);
        log.info("Deactivated flat {}", flatId);
        
        // Save and return updated flat
        Flat updatedFlat = flatRepository.save(flat);
        return flatMapper.toResponse(updatedFlat);
    }
}