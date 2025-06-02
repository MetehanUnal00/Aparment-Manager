package com.example.apartmentmanagerapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for flat responses with integrated contract information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatResponse {
    private Long id;
    private String flatNumber;
    private Integer numberOfRooms;
    private BigDecimal areaSqMeters;
    private Long apartmentBuildingId;
    private String apartmentBuildingName; // For convenience
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Flat status
    private Boolean isActive;
    private BigDecimal currentBalance;
    
    // Active contract information (null if vacant)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ActiveContractInfo activeContract;
    
    // Calculated occupancy status
    private OccupancyStatus occupancyStatus;
    
    // Historical summary (optional - for details view)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OccupancySummary occupancySummary;
    
    /**
     * Occupancy status enum
     */
    public enum OccupancyStatus {
        OCCUPIED("Occupied"),
        VACANT("Vacant"),
        PENDING_MOVE_IN("Pending Move-in");
        
        private final String displayName;
        
        OccupancyStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Occupancy summary for historical data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupancySummary {
        private Integer totalContracts;
        private LocalDate firstOccupancyDate;
        private LocalDate lastVacancyDate;
        private BigDecimal averageRent;
        private Integer totalMonthsOccupied;
    }
}