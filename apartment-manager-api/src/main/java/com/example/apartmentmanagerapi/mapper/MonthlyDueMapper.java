package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.MonthlyDueRequest;
import com.example.apartmentmanagerapi.dto.MonthlyDueResponse;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between MonthlyDue entity and DTOs.
 * Handles mappings for both request and response transformations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MonthlyDueMapper {
    
    /**
     * Maps a MonthlyDue entity to a MonthlyDueResponse DTO.
     * Includes flat and building information, and calculates overdue days.
     * 
     * @param entity the monthly due entity
     * @return the monthly due response DTO
     */
    @Mapping(target = "flat", expression = "java(mapFlatSummary(entity))")
    @Mapping(target = "isOverdue", expression = "java(entity.isOverdue())")
    MonthlyDueResponse toResponse(MonthlyDue entity);
    
    /**
     * Maps a MonthlyDueRequest DTO to a MonthlyDue entity.
     * Note: The flat association must be set separately in the service layer.
     * 
     * @param request the monthly due request DTO
     * @return the monthly due entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "flat", ignore = true) // Set in service layer
    @Mapping(target = "status", constant = "UNPAID")
    @Mapping(target = "paidAmount", constant = "0.00")
    @Mapping(target = "paymentDate", ignore = true)
    MonthlyDue toEntity(MonthlyDueRequest request);
    
    /**
     * Updates an existing MonthlyDue entity from a MonthlyDueRequest DTO.
     * Only allows updating certain fields.
     * 
     * @param request the monthly due request DTO
     * @param entity the existing monthly due entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "flat", ignore = true)
    @Mapping(target = "status", ignore = true) // Status changes through payment processing
    @Mapping(target = "paidAmount", ignore = true) // Updated through payment processing
    @Mapping(target = "paymentDate", ignore = true) // Set when payment is made
    void updateEntityFromRequest(MonthlyDueRequest request, @MappingTarget MonthlyDue entity);
    
    /**
     * Calculates the number of days a monthly due is overdue.
     * Returns 0 if not overdue or already paid.
     * 
     * @param entity the monthly due entity
     * @return number of overdue days
     */
    default Long calculateOverdueDays(MonthlyDue entity) {
        if (entity.getStatus() == MonthlyDue.DueStatus.PAID || 
            entity.getDueDate() == null || 
            entity.getDueDate().isAfter(java.time.LocalDate.now())) {
            return 0L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(entity.getDueDate(), java.time.LocalDate.now());
    }
    
    /**
     * Maps the flat entity to a flat summary for the response.
     * 
     * @param entity the monthly due entity with flat information
     * @return the flat summary DTO
     */
    default MonthlyDueResponse.FlatSummary mapFlatSummary(MonthlyDue entity) {
        if (entity.getFlat() == null) {
            return null;
        }
        return MonthlyDueResponse.FlatSummary.builder()
                .id(entity.getFlat().getId())
                .flatNumber(entity.getFlat().getFlatNumber())
                .tenantName(entity.getFlat().getTenantName())
                .tenantContact(entity.getFlat().getTenantContact())
                .build();
    }
}