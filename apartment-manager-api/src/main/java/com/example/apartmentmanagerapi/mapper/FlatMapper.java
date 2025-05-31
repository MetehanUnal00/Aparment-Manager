package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.entity.Flat;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Flat entity and DTOs.
 * Handles mappings for both request and response transformations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FlatMapper {
    
    /**
     * Maps a Flat entity to a FlatResponse DTO.
     * Includes mapping of apartment building details and calculated fields.
     * 
     * @param entity the flat entity
     * @return the flat response DTO
     */
    @Mapping(source = "apartmentBuilding.id", target = "apartmentBuildingId")
    @Mapping(source = "apartmentBuilding.name", target = "apartmentBuildingName")
    FlatResponse toResponse(Flat entity);
    
    /**
     * Maps a FlatRequest DTO to a Flat entity.
     * Note: The apartmentBuilding association must be set separately in the service layer.
     * 
     * @param request the flat request DTO
     * @return the flat entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "apartmentBuilding", ignore = true) // Set in service layer
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "monthlyDues", ignore = true)
    @Mapping(target = "currentBalance", ignore = true) // Calculated field
    @Mapping(source = "isActive", target = "isActive", defaultValue = "true")
    Flat toEntity(FlatRequest request);
    
    /**
     * Updates an existing Flat entity from a FlatRequest DTO.
     * Preserves system-managed fields and relationships.
     * 
     * @param request the flat request DTO
     * @param entity the existing flat entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "apartmentBuilding", ignore = true) // Only update if explicitly changed
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "monthlyDues", ignore = true)
    @Mapping(target = "currentBalance", ignore = true) // Calculated field
    void updateEntityFromRequest(FlatRequest request, @MappingTarget Flat entity);
    
    /**
     * After mapping callback to set default values.
     * Called automatically by MapStruct after mapping.
     * 
     * @param flat the mapped flat entity
     * @param request the source request
     */
    @AfterMapping
    default void setDefaults(@MappingTarget Flat flat, FlatRequest request) {
        if (flat.getIsActive() == null) {
            flat.setIsActive(true);
        }
    }
}