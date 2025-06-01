package com.example.apartmentmanagerapi.mapper;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.entity.User;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Mapper for Contract entity and DTOs using MapStruct
 */
@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class ContractMapper {
    
    @Autowired
    protected FlatMapper flatMapper;
    
    /**
     * Map ContractRequest to Contract entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flat", source = "flatId", qualifiedByName = "flatIdToFlat")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "monthlyDues", ignore = true)
    @Mapping(target = "previousContract", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancellationDate", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Contract toEntity(ContractRequest request);
    
    /**
     * Map Contract entity to ContractResponse
     */
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "buildingName", source = "flat.apartmentBuilding.name")
    @Mapping(target = "contractLengthInMonths", expression = "java(calculateContractLength(contract))")
    @Mapping(target = "statusDisplayName", source = "status.displayName")
    @Mapping(target = "previousContractId", source = "previousContract.id")
    @Mapping(target = "hasRenewal", expression = "java(hasRenewal(contract))")
    @Mapping(target = "cancelledByUsername", source = "cancelledBy.username")
    @Mapping(target = "totalDuesGenerated", expression = "java(getTotalDuesCount(contract))")
    @Mapping(target = "paidDuesCount", expression = "java(getPaidDuesCount(contract))")
    @Mapping(target = "unpaidDuesCount", expression = "java(getUnpaidDuesCount(contract))")
    @Mapping(target = "totalAmountDue", expression = "java(getTotalAmountDue(contract))")
    @Mapping(target = "totalAmountPaid", expression = "java(getTotalAmountPaid(contract))")
    @Mapping(target = "outstandingBalance", expression = "java(getOutstandingBalance(contract))")
    @Mapping(target = "isExpiringSoon", expression = "java(isExpiringSoon(contract))")
    @Mapping(target = "daysUntilExpiry", expression = "java(getDaysUntilExpiry(contract))")
    @Mapping(target = "hasOverdueDues", expression = "java(hasOverdueDues(contract))")
    @Mapping(target = "nextDueDate", expression = "java(getNextDueDate(contract))")
    @Mapping(target = "isCurrentlyActive", expression = "java(contract.isCurrentlyActive())")
    @Mapping(target = "canBeRenewed", expression = "java(canBeRenewed(contract))")
    @Mapping(target = "canBeModified", expression = "java(canBeModified(contract))")
    @Mapping(target = "canBeCancelled", expression = "java(canBeCancelled(contract))")
    public abstract ContractResponse toResponse(Contract contract);
    
    /**
     * Map Contract entity to ContractSummaryResponse
     */
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "buildingName", source = "flat.apartmentBuilding.name")
    @Mapping(target = "outstandingBalance", expression = "java(getOutstandingBalance(contract))")
    @Mapping(target = "isExpiringSoon", expression = "java(isExpiringSoon(contract))")
    @Mapping(target = "daysUntilExpiry", expression = "java(getDaysUntilExpiry(contract))")
    @Mapping(target = "hasOverdueDues", expression = "java(hasOverdueDues(contract))")
    @Mapping(target = "isCurrentlyActive", expression = "java(contract.isCurrentlyActive())")
    @Mapping(target = "contractLengthInMonths", expression = "java(calculateContractLength(contract))")
    @Mapping(target = "statusBadgeColor", expression = "java(getStatusBadgeColor(contract))")
    public abstract ContractSummaryResponse toSummaryResponse(Contract contract);
    
    /**
     * Map list of contracts to summary responses
     */
    public abstract List<ContractSummaryResponse> toSummaryResponseList(List<Contract> contracts);
    
    /**
     * Update Contract entity from renewal request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "flat", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "endDate", source = "newEndDate")
    @Mapping(target = "monthlyRent", source = "newMonthlyRent", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "dayOfMonth", source = "newDayOfMonth", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "securityDeposit", source = "newSecurityDeposit")
    @Mapping(target = "notes", source = "renewalNotes")
    public abstract void updateFromRenewalRequest(@MappingTarget Contract contract, ContractRenewalRequest request);
    
    /**
     * Create expiry notification from contract
     */
    @Mapping(target = "contractId", source = "id")
    @Mapping(target = "flatId", source = "flat.id")
    @Mapping(target = "flatNumber", source = "flat.flatNumber")
    @Mapping(target = "buildingName", source = "flat.apartmentBuilding.name")
    @Mapping(target = "buildingId", source = "flat.apartmentBuilding.id")
    @Mapping(target = "daysUntilExpiry", expression = "java(getDaysUntilExpiry(contract))")
    @Mapping(target = "urgencyLevel", expression = "java(getUrgencyLevel(contract))")
    @Mapping(target = "outstandingBalance", expression = "java(getOutstandingBalance(contract))")
    @Mapping(target = "hasOverdueDues", expression = "java(hasOverdueDues(contract))")
    @Mapping(target = "assignedManagers", ignore = true) // Set by service
    @Mapping(target = "renewalRecommended", expression = "java(!hasOverdueDues(contract))")
    @Mapping(target = "recommendedAction", expression = "java(getRecommendedAction(contract))")
    public abstract ContractExpiryNotification toExpiryNotification(Contract contract);
    
    // Named mapping methods
    
    @Named("flatIdToFlat")
    protected Flat flatIdToFlat(Long flatId) {
        if (flatId == null) return null;
        Flat flat = new Flat();
        flat.setId(flatId);
        return flat;
    }
    
    // Helper methods for calculated fields
    
    protected Integer calculateContractLength(Contract contract) {
        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            return null;
        }
        return (int) ChronoUnit.MONTHS.between(contract.getStartDate(), contract.getEndDate()) + 1;
    }
    
    protected boolean hasRenewal(Contract contract) {
        // This would need to be implemented with repository access
        return false;
    }
    
    protected Integer getTotalDuesCount(Contract contract) {
        return contract.getMonthlyDues() != null ? contract.getMonthlyDues().size() : 0;
    }
    
    protected Integer getPaidDuesCount(Contract contract) {
        if (contract.getMonthlyDues() == null) return 0;
        return (int) contract.getMonthlyDues().stream()
            .filter(due -> due.getStatus() == MonthlyDue.DueStatus.PAID)
            .count();
    }
    
    protected Integer getUnpaidDuesCount(Contract contract) {
        if (contract.getMonthlyDues() == null) return 0;
        return (int) contract.getMonthlyDues().stream()
            .filter(due -> due.getStatus() == MonthlyDue.DueStatus.UNPAID)
            .count();
    }
    
    protected BigDecimal getTotalAmountDue(Contract contract) {
        if (contract.getMonthlyDues() == null) return BigDecimal.ZERO;
        return contract.getMonthlyDues().stream()
            .map(MonthlyDue::getDueAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    protected BigDecimal getTotalAmountPaid(Contract contract) {
        if (contract.getMonthlyDues() == null) return BigDecimal.ZERO;
        return contract.getMonthlyDues().stream()
            .filter(due -> due.getStatus() == MonthlyDue.DueStatus.PAID)
            .map(MonthlyDue::getDueAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    protected BigDecimal getOutstandingBalance(Contract contract) {
        return getTotalAmountDue(contract).subtract(getTotalAmountPaid(contract));
    }
    
    protected boolean isExpiringSoon(Contract contract) {
        if (contract.getEndDate() == null || contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            return false;
        }
        long daysUntilExpiry = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        return daysUntilExpiry >= 0 && daysUntilExpiry <= 30;
    }
    
    protected Integer getDaysUntilExpiry(Contract contract) {
        if (contract.getEndDate() == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        return days >= 0 ? (int) days : null;
    }
    
    protected boolean hasOverdueDues(Contract contract) {
        if (contract.getMonthlyDues() == null) return false;
        LocalDate today = LocalDate.now();
        return contract.getMonthlyDues().stream()
            .anyMatch(due -> due.getStatus() == MonthlyDue.DueStatus.UNPAID && 
                           due.getDueDate() != null && 
                           due.getDueDate().isBefore(today));
    }
    
    protected LocalDate getNextDueDate(Contract contract) {
        if (contract.getMonthlyDues() == null) return null;
        LocalDate today = LocalDate.now();
        return contract.getMonthlyDues().stream()
            .filter(due -> due.getStatus() == MonthlyDue.DueStatus.UNPAID)
            .map(MonthlyDue::getDueDate)
            .filter(date -> date != null && !date.isBefore(today))
            .min(LocalDate::compareTo)
            .orElse(null);
    }
    
    protected boolean canBeRenewed(Contract contract) {
        return contract.getStatus() == Contract.ContractStatus.ACTIVE && 
               isExpiringSoon(contract) && 
               !hasOverdueDues(contract);
    }
    
    protected boolean canBeModified(Contract contract) {
        return contract.getStatus() == Contract.ContractStatus.ACTIVE ||
               contract.getStatus() == Contract.ContractStatus.PENDING;
    }
    
    protected boolean canBeCancelled(Contract contract) {
        return contract.getStatus() == Contract.ContractStatus.ACTIVE ||
               contract.getStatus() == Contract.ContractStatus.PENDING;
    }
    
    protected String getStatusBadgeColor(Contract contract) {
        switch (contract.getStatus()) {
            case ACTIVE:
                return isExpiringSoon(contract) ? "warning" : "success";
            case PENDING:
                return "info";
            case EXPIRED:
            case CANCELLED:
            case SUPERSEDED:
                return "danger";
            case RENEWED:
                return "secondary";
            default:
                return "light";
        }
    }
    
    protected String getUrgencyLevel(Contract contract) {
        Integer days = getDaysUntilExpiry(contract);
        if (days == null) return "INFO";
        if (days < 7) return "URGENT";
        if (days < 14) return "WARNING";
        return "INFO";
    }
    
    protected String getRecommendedAction(Contract contract) {
        if (hasOverdueDues(contract)) {
            return "Resolve overdue payments before renewal";
        }
        if (isExpiringSoon(contract)) {
            return "Renew contract soon";
        }
        return "No action required";
    }
}