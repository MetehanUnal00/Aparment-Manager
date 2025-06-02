# Backend Contract Integration Plan - Adding Contract Info to FlatResponse

## Overview
This document outlines the comprehensive plan for enhancing the backend to include active contract information directly in the FlatResponse DTO, eliminating the need for separate API calls in the frontend.

## Phase 1: Backend Database & Performance Analysis

### 1.1 Database Optimization
```sql
-- Add indexes for performance
CREATE INDEX idx_contract_flat_status ON contracts(flat_id, status) 
  WHERE status IN ('ACTIVE', 'PENDING');

CREATE INDEX idx_contract_dates_status ON contracts(start_date, end_date, status);

-- Consider a materialized view for current occupancy
CREATE MATERIALIZED VIEW flat_current_occupancy AS
SELECT 
  f.id as flat_id,
  c.id as contract_id,
  c.tenant_name,
  c.tenant_contact,
  c.tenant_email,
  c.monthly_rent,
  c.start_date,
  c.end_date,
  c.status as contract_status
FROM flats f
LEFT JOIN contracts c ON f.id = c.flat_id 
  AND c.status = 'ACTIVE'
  AND CURRENT_DATE BETWEEN c.start_date AND c.end_date;
```

### 1.2 Repository Layer Enhancement
```java
// FlatRepository.java
@Query("SELECT f FROM Flat f LEFT JOIN FETCH f.apartmentBuilding WHERE f.id = :id")
Optional<Flat> findByIdWithBuilding(@Param("id") Long id);

@Query("""
    SELECT f FROM Flat f 
    LEFT JOIN FETCH f.apartmentBuilding 
    WHERE f.apartmentBuilding.id = :buildingId 
    ORDER BY f.flatNumber
    """)
List<Flat> findByBuildingIdWithBuilding(@Param("buildingId") Long buildingId);

// ContractRepository.java - Add efficient batch loading
@Query("""
    SELECT c FROM Contract c 
    WHERE c.flat.id IN :flatIds 
    AND c.status = 'ACTIVE'
    AND CURRENT_DATE BETWEEN c.startDate AND c.endDate
    """)
List<Contract> findActiveContractsByFlatIds(@Param("flatIds") List<Long> flatIds);
```

## Phase 2: DTO Structure Updates

### 2.1 Create Embedded DTO for Contract Info
```java
// ActiveContractInfo.java
package com.example.apartmentmanagerapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveContractInfo {
    private Long contractId;
    private String tenantName;
    private String tenantEmail;
    private String tenantContact;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate moveInDate; // Same as startDate but clearer naming
    private Integer daysUntilExpiry;
    private boolean isExpiringSoon;
    private String contractStatus;
}
```

### 2.2 Update FlatResponse
```java
// FlatResponse.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlatResponse {
    // ... existing fields ...
    
    // Active contract information (null if vacant)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ActiveContractInfo activeContract;
    
    // Calculated occupancy status
    private OccupancyStatus occupancyStatus; // OCCUPIED, VACANT, PENDING_MOVE_IN
    
    // Historical summary (optional - for details view)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OccupancySummary occupancySummary;
    
    public enum OccupancyStatus {
        OCCUPIED("Occupied"),
        VACANT("Vacant"),
        PENDING_MOVE_IN("Pending Move-in");
        
        private final String displayName;
        // constructor, getter
    }
    
    @Data
    @Builder
    public static class OccupancySummary {
        private Integer totalContracts;
        private LocalDate firstOccupancyDate;
        private LocalDate lastVacancyDate;
        private BigDecimal averageRent;
        private Integer totalMonthsOccupied;
    }
}
```

## Phase 3: Service Layer Enhancement

### 3.1 Create Contract Loading Strategy
```java
// ContractLoadingService.java
@Service
@RequiredArgsConstructor
public class ContractLoadingService {
    private final ContractRepository contractRepository;
    
    /**
     * Efficiently load active contracts for multiple flats
     */
    public Map<Long, Contract> loadActiveContractsForFlats(List<Long> flatIds) {
        if (flatIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<Contract> activeContracts = contractRepository
            .findActiveContractsByFlatIds(flatIds);
            
        return activeContracts.stream()
            .collect(Collectors.toMap(
                c -> c.getFlat().getId(),
                c -> c,
                (existing, replacement) -> existing // Handle duplicates
            ));
    }
    
    /**
     * Load contract history summary for flats
     */
    @Cacheable(value = "flatOccupancySummary", key = "#flatId")
    public OccupancySummary loadOccupancySummary(Long flatId) {
        // Implementation for historical data
    }
}
```

### 3.2 Update FlatService
```java
// FlatService.java
@Service
@RequiredArgsConstructor
public class FlatService implements IFlatService {
    private final FlatRepository flatRepository;
    private final ContractLoadingService contractLoadingService;
    private final FlatMapper flatMapper;
    
    @Override
    @Transactional(readOnly = true)
    public List<FlatResponse> getFlatsByBuilding(Long buildingId) {
        // Load flats
        List<Flat> flats = flatRepository.findByBuildingIdWithBuilding(buildingId);
        
        // Extract flat IDs
        List<Long> flatIds = flats.stream()
            .map(Flat::getId)
            .collect(Collectors.toList());
        
        // Batch load active contracts
        Map<Long, Contract> activeContracts = contractLoadingService
            .loadActiveContractsForFlats(flatIds);
        
        // Map to responses with contract info
        return flats.stream()
            .map(flat -> {
                FlatResponse response = flatMapper.toResponse(flat);
                Contract activeContract = activeContracts.get(flat.getId());
                
                if (activeContract != null) {
                    response.setActiveContract(mapToActiveContractInfo(activeContract));
                    response.setOccupancyStatus(OccupancyStatus.OCCUPIED);
                } else {
                    response.setOccupancyStatus(OccupancyStatus.VACANT);
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }
    
    private ActiveContractInfo mapToActiveContractInfo(Contract contract) {
        return ActiveContractInfo.builder()
            .contractId(contract.getId())
            .tenantName(contract.getTenantName())
            .tenantEmail(contract.getTenantEmail())
            .tenantContact(contract.getTenantContact())
            .monthlyRent(contract.getMonthlyRent())
            .securityDeposit(contract.getSecurityDeposit())
            .startDate(contract.getStartDate())
            .endDate(contract.getEndDate())
            .moveInDate(contract.getStartDate())
            .daysUntilExpiry(calculateDaysUntilExpiry(contract))
            .isExpiringSoon(isExpiringSoon(contract))
            .contractStatus(contract.getStatus().name())
            .build();
    }
}
```

## Phase 4: Caching Strategy

### 4.1 Cache Configuration
```java
// CacheConfig.java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Cache for flat responses with contract info
        cacheManager.registerCustomCache("flatsWithContracts",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build());
                
        // Cache for individual flat contract info
        cacheManager.registerCustomCache("flatActiveContract",
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build());
                
        return cacheManager;
    }
}
```

### 4.2 Cache Invalidation
```java
// ContractEventListener.java - Update to invalidate caches
@EventListener
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleContractCreated(ContractCreatedEvent event) {
    // ... existing logic ...
    
    // Invalidate flat cache
    evictFlatCache(event.getContract().getFlat().getId());
    evictBuildingFlatsCache(event.getContract().getFlat().getApartmentBuilding().getId());
}

@CacheEvict(value = "flatsWithContracts", key = "#buildingId")
public void evictBuildingFlatsCache(Long buildingId) {
    log.debug("Evicted flats cache for building: {}", buildingId);
}
```

## Phase 5: Frontend Model Updates

### 5.1 Update TypeScript Models
```typescript
// flat.model.ts
export interface ActiveContractInfo {
  contractId: number;
  tenantName?: string;
  tenantEmail?: string;
  tenantContact?: string;
  monthlyRent: number;
  securityDeposit: number;
  startDate: string;
  endDate: string;
  moveInDate: string;
  daysUntilExpiry?: number;
  isExpiringSoon: boolean;
  contractStatus: string;
}

export interface OccupancySummary {
  totalContracts: number;
  firstOccupancyDate?: string;
  lastVacancyDate?: string;
  averageRent: number;
  totalMonthsOccupied: number;
}

export type OccupancyStatus = 'OCCUPIED' | 'VACANT' | 'PENDING_MOVE_IN';

export interface FlatResponse {
  // ... existing fields ...
  
  // Contract information
  activeContract?: ActiveContractInfo;
  occupancyStatus: OccupancyStatus;
  occupancySummary?: OccupancySummary;
}
```

## Phase 6: Frontend Component Updates

### 6.1 Update Flat List Component
```typescript
// flat-list.component.ts
export class FlatListComponent {
  // Remove any contract fetching logic
  
  getOccupancyBadgeClass(status: OccupancyStatus): string {
    const classes = {
      'OCCUPIED': 'badge-success',
      'VACANT': 'badge-warning',
      'PENDING_MOVE_IN': 'badge-info'
    };
    return classes[status] || 'badge-secondary';
  }
  
  getTenantDisplay(flat: FlatResponse): string {
    return flat.activeContract?.tenantName || 'Vacant';
  }
}
```

### 6.2 Update Flat List Template
```html
<!-- flat-list.component.html -->
<td>
  <div class="d-flex align-items-center">
    <span>{{ flat.activeContract?.tenantName || '-' }}</span>
    <span class="badge ms-2" [ngClass]="getOccupancyBadgeClass(flat.occupancyStatus)">
      {{ flat.occupancyStatus }}
    </span>
  </div>
</td>
<td>{{ flat.activeContract?.monthlyRent | currencyFormat }}</td>
<td>
  <span *ngIf="flat.activeContract?.tenantContact">
    {{ flat.activeContract.tenantContact | phoneFormat }}
  </span>
  <span *ngIf="!flat.activeContract?.tenantContact">-</span>
</td>
```

## Phase 7: API Endpoint Updates

### 7.1 Add Specialized Endpoints
```java
// FlatController.java
@GetMapping("/building/{buildingId}/occupancy-summary")
@PreAuthorize("hasRole('ROLE_VIEWER')")
public ResponseEntity<BuildingOccupancySummary> getBuildingOccupancySummary(
        @PathVariable Long buildingId) {
    BuildingOccupancySummary summary = flatService.getBuildingOccupancySummary(buildingId);
    return ResponseEntity.ok(summary);
}

@GetMapping("/{id}/with-history")
@PreAuthorize("hasRole('ROLE_VIEWER')")
public ResponseEntity<FlatDetailedResponse> getFlatWithHistory(
        @PathVariable Long id,
        @RequestParam(defaultValue = "false") boolean includeHistory) {
    FlatDetailedResponse response = flatService.getFlatWithHistory(id, includeHistory);
    return ResponseEntity.ok(response);
}
```

## Phase 8: Migration Strategy

1. **Deploy Backend First**: Deploy with new fields but keep them nullable
2. **Update Frontend**: Deploy frontend that can handle both old and new responses
3. **Enable Feature**: Turn on contract loading via feature flag
4. **Monitor Performance**: Watch for any performance degradation
5. **Remove Old Code**: Clean up any client-side contract fetching

## Phase 9: Testing Strategy

```java
// FlatServiceIntegrationTest.java
@Test
@Sql("/test-data/flats-with-contracts.sql")
public void testGetFlatsWithActiveContracts() {
    // Given building with 3 flats: 2 occupied, 1 vacant
    Long buildingId = 1L;
    
    // When
    List<FlatResponse> flats = flatService.getFlatsByBuilding(buildingId);
    
    // Then
    assertThat(flats).hasSize(3);
    
    FlatResponse occupiedFlat = flats.stream()
        .filter(f -> f.getFlatNumber().equals("101"))
        .findFirst().orElseThrow();
        
    assertThat(occupiedFlat.getOccupancyStatus()).isEqualTo(OccupancyStatus.OCCUPIED);
    assertThat(occupiedFlat.getActiveContract()).isNotNull();
    assertThat(occupiedFlat.getActiveContract().getTenantName()).isEqualTo("John Doe");
    
    FlatResponse vacantFlat = flats.stream()
        .filter(f -> f.getFlatNumber().equals("103"))
        .findFirst().orElseThrow();
        
    assertThat(vacantFlat.getOccupancyStatus()).isEqualTo(OccupancyStatus.VACANT);
    assertThat(vacantFlat.getActiveContract()).isNull();
}
```

## Performance Considerations

1. **Batch Loading**: Use single query to load contracts for all flats in a building
2. **Caching**: Cache results for 5-10 minutes with proper invalidation
3. **Lazy Loading**: Only load contract history when specifically requested
4. **Database Indexes**: Ensure proper indexes on flat_id and status columns

## Benefits

1. **Reduced API Calls**: Single call to get flats with contract info
2. **Better Performance**: Batch loading prevents N+1 queries
3. **Data Consistency**: Contract info always in sync with flat data
4. **Simplified Frontend**: No need for complex data joining in UI
5. **Cache Efficiency**: Better cache hit rates with combined data

## Implementation Timeline

1. Week 1: Backend implementation (DTOs, Services, Repositories)
2. Week 2: Testing and performance optimization
3. Week 3: Frontend updates and integration testing
4. Week 4: Deployment and monitoring