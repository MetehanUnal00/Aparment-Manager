package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.Contract.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Contract entity with custom queries
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    
    /**
     * Find active contract for a flat
     */
    Optional<Contract> findByFlatIdAndStatus(Long flatId, ContractStatus status);
    
    /**
     * Check if flat has an active contract
     */
    boolean existsByFlatIdAndStatus(Long flatId, ContractStatus status);
    
    /**
     * Find all contracts for a flat
     */
    List<Contract> findByFlatIdOrderByStartDateDesc(Long flatId);
    
    /**
     * Find contracts by building with pagination
     */
    @Query("SELECT c FROM Contract c WHERE c.flat.apartmentBuilding.id = :buildingId ORDER BY c.createdAt DESC")
    Page<Contract> findByBuildingId(@Param("buildingId") Long buildingId, Pageable pageable);
    
    /**
     * Find contracts expiring within days
     */
    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' AND c.endDate BETWEEN :today AND :futureDate")
    List<Contract> findExpiringContracts(@Param("today") LocalDate today, 
                                        @Param("futureDate") LocalDate futureDate);
    
    /**
     * Find contracts that need status update
     */
    @Query("SELECT c FROM Contract c WHERE " +
           "(c.status = 'PENDING' AND c.startDate <= :today) OR " +
           "(c.status = 'ACTIVE' AND c.endDate < :today)")
    List<Contract> findContractsNeedingStatusUpdate(@Param("today") LocalDate today);
    
    /**
     * Update contract status in bulk
     */
    @Modifying
    @Query("UPDATE Contract c SET c.status = :newStatus WHERE c.id IN :contractIds")
    int updateContractStatusBulk(@Param("contractIds") List<Long> contractIds, 
                                @Param("newStatus") ContractStatus newStatus);
    
    /**
     * Find overlapping contracts for validation
     */
    @Query("SELECT c FROM Contract c WHERE c.flat.id = :flatId " +
           "AND c.status NOT IN ('CANCELLED', 'SUPERSEDED') " +
           "AND c.id != :excludeContractId " +
           "AND ((c.startDate <= :endDate AND c.endDate >= :startDate))")
    List<Contract> findOverlappingContracts(@Param("flatId") Long flatId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("excludeContractId") Long excludeContractId);
    
    /**
     * Find contracts with overdue payments
     */
    @Query("SELECT DISTINCT c FROM Contract c " +
           "JOIN c.monthlyDues md " +
           "WHERE c.status = 'ACTIVE' " +
           "AND md.status = 'UNPAID' " +
           "AND md.dueDate < :today")
    List<Contract> findContractsWithOverdueDues(@Param("today") LocalDate today);
    
    /**
     * Get contract statistics for a building
     */
    @Query("SELECT COUNT(c) as total, " +
           "SUM(CASE WHEN c.status = 'ACTIVE' THEN 1 ELSE 0 END) as active, " +
           "SUM(CASE WHEN c.status = 'EXPIRED' THEN 1 ELSE 0 END) as expired, " +
           "SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) as pending " +
           "FROM Contract c WHERE c.flat.apartmentBuilding.id = :buildingId")
    Object getContractStatisticsByBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Find contracts by tenant name (search)
     */
    @Query("SELECT c FROM Contract c WHERE LOWER(c.tenantName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Contract> searchByTenantName(@Param("search") String search, Pageable pageable);
    
    /**
     * Find renewable contracts (expiring soon and in good standing)
     */
    @Query("SELECT c FROM Contract c WHERE c.status = 'ACTIVE' " +
           "AND c.endDate BETWEEN :startDate AND :endDate " +
           "AND NOT EXISTS (SELECT md FROM c.monthlyDues md WHERE md.status = 'UNPAID' AND md.dueDate < :today)")
    List<Contract> findRenewableContracts(@Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate,
                                         @Param("today") LocalDate today);
    
    /**
     * Get total monthly rent for active contracts in a building
     */
    @Query("SELECT SUM(c.monthlyRent) FROM Contract c " +
           "WHERE c.flat.apartmentBuilding.id = :buildingId " +
           "AND c.status = 'ACTIVE'")
    BigDecimal getTotalMonthlyRentByBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Find contracts for notification (with manager info)
     */
    @Query("SELECT DISTINCT c FROM Contract c " +
           "JOIN FETCH c.flat f " +
           "JOIN FETCH f.apartmentBuilding ab " +
           "WHERE c.id IN :contractIds")
    List<Contract> findContractsForNotification(@Param("contractIds") List<Long> contractIds);
    
    /**
     * Find active contracts for multiple flats (batch loading)
     * Used to efficiently load contract information when displaying flat lists
     */
    @Query("SELECT c FROM Contract c " +
           "JOIN FETCH c.flat f " +
           "WHERE c.flat.id IN :flatIds " +
           "AND c.status = 'ACTIVE' " +
           "AND CURRENT_DATE BETWEEN c.startDate AND c.endDate")
    List<Contract> findActiveContractsByFlatIds(@Param("flatIds") List<Long> flatIds);
}