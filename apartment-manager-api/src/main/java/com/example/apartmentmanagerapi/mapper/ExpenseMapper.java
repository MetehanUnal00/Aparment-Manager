package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.ExpenseRequest;
import com.example.apartmentmanagerapi.dto.ExpenseResponse;
import com.example.apartmentmanagerapi.entity.Expense;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Expense entity and DTOs.
 * Handles mappings for both request and response transformations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ExpenseMapper {
    
    /**
     * Maps an Expense entity to an ExpenseResponse DTO.
     * Includes building information in the response.
     * 
     * @param entity the expense entity
     * @return the expense response DTO
     */
    @Mapping(source = "category.displayName", target = "categoryDisplayName")
    @Mapping(source = "recordedBy.username", target = "recordedBy")
    @Mapping(target = "building", expression = "java(mapBuildingSummary(entity))")
    ExpenseResponse toResponse(Expense entity);
    
    /**
     * Maps an ExpenseRequest DTO to an Expense entity.
     * Note: The apartmentBuilding association must be set separately in the service layer.
     * 
     * @param request the expense request DTO
     * @return the expense entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "building", ignore = true) // Set in service layer
    @Mapping(target = "recordedBy", ignore = true) // Set in service layer
    @Mapping(source = "expenseDate", target = "expenseDate", defaultExpression = "java(java.time.LocalDate.now())")
    Expense toEntity(ExpenseRequest request);
    
    /**
     * Updates an existing Expense entity from an ExpenseRequest DTO.
     * Preserves system-managed fields.
     * 
     * @param request the expense request DTO
     * @param entity the existing expense entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "building", ignore = true) // Cannot change building after creation
    @Mapping(target = "recordedBy", ignore = true) // Cannot change who recorded it
    void updateEntityFromRequest(ExpenseRequest request, @MappingTarget Expense entity);
    
    /**
     * Maps the building entity to a building summary for the response.
     * 
     * @param entity the expense entity with building information
     * @return the building summary DTO
     */
    default ExpenseResponse.BuildingSummary mapBuildingSummary(Expense entity) {
        if (entity.getBuilding() == null) {
            return null;
        }
        return ExpenseResponse.BuildingSummary.builder()
                .id(entity.getBuilding().getId())
                .name(entity.getBuilding().getName())
                .address(entity.getBuilding().getAddress())
                .build();
    }
}