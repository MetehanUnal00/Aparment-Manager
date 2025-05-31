package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;

import java.util.List;

/**
 * Service interface for managing apartment buildings.
 * Handles all business logic related to apartment building operations.
 */
public interface IApartmentBuildingService {

    /**
     * Creates a new apartment building.
     * 
     * @param request the apartment building request containing name and address
     * @return the created apartment building response
     * @throws RuntimeException if building name already exists
     */
    ApartmentBuildingResponse createApartmentBuilding(ApartmentBuildingRequest request);

    /**
     * Retrieves all apartment buildings.
     * 
     * @return list of all apartment building responses
     */
    List<ApartmentBuildingResponse> getAllApartmentBuildings();

    /**
     * Retrieves an apartment building by its ID.
     * 
     * @param id the apartment building ID
     * @return the apartment building response
     * @throws RuntimeException if building not found
     */
    ApartmentBuildingResponse getApartmentBuildingById(Long id);

    /**
     * Updates an existing apartment building.
     * 
     * @param id the apartment building ID to update
     * @param request the update request containing new name and address
     * @return the updated apartment building response
     * @throws RuntimeException if building not found or new name already exists
     */
    ApartmentBuildingResponse updateApartmentBuilding(Long id, ApartmentBuildingRequest request);

    /**
     * Deletes an apartment building by its ID.
     * 
     * @param id the apartment building ID to delete
     * @throws RuntimeException if building not found
     */
    void deleteApartmentBuilding(Long id);
}