package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.PaymentRequest;
import com.example.apartmentmanagerapi.dto.PaymentResponse;
import com.example.apartmentmanagerapi.entity.Payment;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Payment entity and DTOs.
 * Handles mappings for both request and response transformations.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {
    
    /**
     * Maps a Payment entity to a PaymentResponse DTO.
     * Includes flat and tenant information in the response.
     * 
     * @param entity the payment entity
     * @return the payment response DTO
     */
    @Mapping(source = "flat.id", target = "flat.id")
    @Mapping(source = "flat.flatNumber", target = "flat.flatNumber")
    @Mapping(source = "flat.tenantName", target = "flat.tenantName")
    @Mapping(source = "flat.apartmentBuilding.id", target = "flat.buildingId")
    @Mapping(source = "flat.apartmentBuilding.name", target = "flat.buildingName")
    @Mapping(source = "recordedBy.username", target = "recordedBy")
    PaymentResponse toResponse(Payment entity);
    
    /**
     * Maps a PaymentRequest DTO to a Payment entity.
     * Note: The flat association must be set separately in the service layer.
     * 
     * @param request the payment request DTO
     * @return the payment entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true) // Optimistic locking version
    @Mapping(target = "flat", ignore = true) // Set in service layer
    @Mapping(target = "recordedBy", ignore = true) // Set in controller
    @Mapping(source = "paymentDate", target = "paymentDate", defaultExpression = "java(java.time.LocalDateTime.now())")
    Payment toEntity(PaymentRequest request);
    
    /**
     * Updates an existing Payment entity from a PaymentRequest DTO.
     * Only updates fields that are allowed to be modified after creation.
     * 
     * @param request the payment request DTO
     * @param entity the existing payment entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true) // Managed by JPA
    @Mapping(target = "flat", ignore = true) // Cannot change flat after creation
    @Mapping(target = "amount", ignore = true) // Amount typically shouldn't change
    @Mapping(target = "paymentDate", ignore = true) // Date typically shouldn't change
    @Mapping(target = "recordedBy", ignore = true) // Cannot change who recorded it
    void updateEntityFromRequest(PaymentRequest request, @MappingTarget Payment entity);
}