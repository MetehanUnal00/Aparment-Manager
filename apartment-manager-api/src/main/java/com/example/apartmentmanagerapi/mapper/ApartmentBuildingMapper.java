package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;
import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between ApartmentBuilding entity and DTOs.
 * Handles mappings for both request and response transformations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApartmentBuildingMapper {
    
    /**
     * Maps an ApartmentBuilding entity to an ApartmentBuildingResponse DTO.
     * 
     * @param entity the apartment building entity
     * @return the apartment building response DTO
     */
    ApartmentBuildingResponse toResponse(ApartmentBuilding entity);
    
    /**
     * Maps an ApartmentBuildingRequest DTO to an ApartmentBuilding entity.
     * Ignores ID and audit fields which are managed by the system.
     * 
     * @param request the apartment building request DTO
     * @return the apartment building entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "flats", ignore = true)
    @Mapping(target = "userAssignments", ignore = true)
    @Mapping(target = "expenses", ignore = true)
    ApartmentBuilding toEntity(ApartmentBuildingRequest request);
    
    /**
     * Updates an existing ApartmentBuilding entity from an ApartmentBuildingRequest DTO.
     * Preserves system-managed fields while updating user-editable fields.
     * 
     * @param request the apartment building request DTO
     * @param entity the existing apartment building entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "flats", ignore = true)
    @Mapping(target = "userAssignments", ignore = true)
    @Mapping(target = "expenses", ignore = true)
    void updateEntityFromRequest(ApartmentBuildingRequest request, @MappingTarget ApartmentBuilding entity);
}