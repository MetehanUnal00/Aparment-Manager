package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing apartment flats.
 * Handles flat creation, tenant management, and financial information.
 */
public interface IFlatService {

    /**
     * Creates a new flat in an apartment building.
     * 
     * @param request the flat request containing flat details and tenant information
     * @return the created flat response
     * @throws RuntimeException if building not found or flat number already exists
     */
    FlatResponse createFlat(FlatRequest request);

    /**
     * Retrieves all flats in a specific building.
     * 
     * @param buildingId the apartment building ID
     * @return list of all flat responses in the building
     * @throws RuntimeException if building not found
     */
    List<FlatResponse> getAllFlatsByBuildingId(Long buildingId);

    /**
     * Retrieves a specific flat by building ID and flat ID.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID
     * @return the flat response
     * @throws RuntimeException if flat not found
     */
    FlatResponse getFlatById(Long buildingId, Long flatId);

    /**
     * Updates an existing flat.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID to update
     * @param request the update request containing new flat details
     * @return the updated flat response
     * @throws RuntimeException if flat or building not found, or new flat number already exists
     */
    FlatResponse updateFlat(Long buildingId, Long flatId, FlatRequest request);

    /**
     * Deletes a flat from a building.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID to delete
     * @throws RuntimeException if flat not found
     */
    void deleteFlat(Long buildingId, Long flatId);

    /**
     * Get flat with additional financial information.
     * Includes current balance and recent payment history.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID
     * @return map containing flat info, balance, payments, and dues
     * @throws RuntimeException if flat not found
     */
    Map<String, Object> getFlatWithFinancialInfo(Long buildingId, Long flatId);

    /**
     * Get all active flats in a building.
     * Only returns flats where isActive = true.
     * 
     * @param buildingId the apartment building ID
     * @return list of active flat responses
     * @throws RuntimeException if building not found
     */
    List<FlatResponse> getActiveFlatsByBuildingId(Long buildingId);

    /**
     * Update tenant information for a flat.
     * Used when a new tenant moves in or existing tenant info changes.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID
     * @param request the request containing new tenant information
     * @return the updated flat response
     * @throws RuntimeException if flat not found
     */
    FlatResponse updateTenantInfo(Long buildingId, Long flatId, FlatRequest request);

    /**
     * Mark a flat as inactive.
     * Inactive flats won't be included in monthly due generation.
     * 
     * @param buildingId the apartment building ID
     * @param flatId the flat ID to deactivate
     * @return the updated flat response
     * @throws RuntimeException if flat not found
     */
    FlatResponse deactivateFlat(Long buildingId, Long flatId);
}