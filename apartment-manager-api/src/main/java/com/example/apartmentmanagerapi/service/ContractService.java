package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.*;
import com.example.apartmentmanagerapi.event.*;
import com.example.apartmentmanagerapi.exception.*;
import com.example.apartmentmanagerapi.mapper.ContractMapper;
import com.example.apartmentmanagerapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service implementation for contract management
 * Handles contract lifecycle: creation, renewal, cancellation, and modification
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractService implements IContractService {

    private final ContractRepository contractRepository;
    private final FlatRepository flatRepository;
    private final UserRepository userRepository;
    private final ContractMapper contractMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IAuditService auditService;

    @Override
    public ContractResponse createContract(ContractRequest request) {
        log.info("Creating new contract for flat ID: {}, generateDuesImmediately: {}", 
                request.getFlatId(), request.isGenerateDuesImmediately());
        
        // Validate flat exists and is active
        Flat flat = flatRepository.findById(request.getFlatId())
            .orElseThrow(() -> new ResourceNotFoundException("Flat", request.getFlatId()));
        
        if (!flat.getIsActive()) {
            throw new BusinessRuleException("Cannot create contract for inactive flat");
        }
        
        // Validate dates
        validateContractDates(request.getStartDate(), request.getEndDate());
        
        // Validate day of month
        if (request.getDayOfMonth() < 1 || request.getDayOfMonth() > 31) {
            throw new ValidationException("Invalid day of month: " + request.getDayOfMonth());
        }
        
        // Check for overlapping contracts
        List<Contract> overlapping = contractRepository.findOverlappingContracts(
            request.getFlatId(), request.getStartDate(), request.getEndDate(), 0L);
        
        if (!overlapping.isEmpty()) {
            throw new ContractOverlapException("Flat has overlapping contracts for the specified period");
        }
        
        // Create contract entity
        Contract contract = contractMapper.toEntity(request);
        contract.setFlat(flat);
        contract.setStatus(Contract.ContractStatus.ACTIVE);
        
        // Set deposit amount
        contract.setDepositAmount(request.getSecurityDeposit());
        
        // Save contract
        contract = contractRepository.save(contract);
        
        // Get current user ID
        String currentUsername = getCurrentUsername();
        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        // Publish event for due generation
        log.info("Publishing ContractCreatedEvent for contract ID: {}, generateDues: {}, userId: {}", 
                contract.getId(), request.isGenerateDuesImmediately(), userId);
        eventPublisher.publishEvent(new ContractCreatedEvent(
            this, contract, request.isGenerateDuesImmediately(), userId));
        log.info("ContractCreatedEvent published successfully");
        
        // Audit log
        auditService.logSuccess(
            AuditLog.AuditAction.CONTRACT_CREATED,
            "Contract",
            contract.getId(),
            String.format("Created contract for flat %s from %s to %s", 
                flat.getFlatNumber(), request.getStartDate(), request.getEndDate())
        );
        
        return contractMapper.toResponse(contract);
    }

    @Override
    public ContractResponse renewContract(Long contractId, ContractRenewalRequest request) {
        log.info("Renewing contract ID: {}", contractId);
        
        // Find existing contract
        Contract existingContract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException(contractId));
        
        // Validate contract can be renewed
        if (existingContract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw new BusinessRuleException("Only active contracts can be renewed");
        }
        
        // Validate new end date
        if (request.getNewEndDate() != null && 
            !request.getNewEndDate().isAfter(existingContract.getEndDate())) {
            throw new ValidationException("New end date must be after current end date");
        }
        
        // Create renewal contract
        Contract renewalContract = Contract.builder()
            .flat(existingContract.getFlat())
            .tenant(existingContract.getTenant())
            .tenantName(existingContract.getTenantName())
            .tenantContact(existingContract.getTenantContact())
            .tenantEmail(existingContract.getTenantEmail())
            .startDate(existingContract.getEndDate().plusDays(1))
            .endDate(request.getNewEndDate() != null ? 
                request.getNewEndDate() : existingContract.getEndDate().plusYears(1))
            .monthlyRent(request.getNewMonthlyRent() != null ? 
                request.getNewMonthlyRent() : existingContract.getMonthlyRent())
            .dayOfMonth(existingContract.getDayOfMonth())
            .depositAmount(existingContract.getDepositAmount())
            .autoRenew(existingContract.isAutoRenew())
            .status(Contract.ContractStatus.PENDING)
            .previousContract(existingContract)
            .notes(request.getRenewalNotes())
            .build();
        
        // Save renewal contract
        renewalContract = contractRepository.save(renewalContract);
        
        // Update existing contract status
        existingContract.setStatus(Contract.ContractStatus.RENEWED);
        existingContract.setStatusChangedAt(LocalDateTime.now());
        existingContract.setStatusChangedBy(getCurrentUsername());
        existingContract.setStatusChangeReason("Renewed with contract ID: " + renewalContract.getId());
        contractRepository.save(existingContract);
        
        // Get current user ID
        String username = getCurrentUsername();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        // Publish renewal event
        eventPublisher.publishEvent(new ContractRenewedEvent(
            this, existingContract, renewalContract, request.isGenerateDuesImmediately(), userId));
        
        // Audit log
        auditService.logSuccess(
            AuditLog.AuditAction.CONTRACT_RENEWED,
            "Contract",
            renewalContract.getId(),
            String.format("Renewed contract %d with new contract %d", 
                existingContract.getId(), renewalContract.getId())
        );
        
        return contractMapper.toResponse(renewalContract);
    }

    @Override
    public ContractResponse cancelContract(Long contractId, ContractCancellationRequest request) {
        log.info("Cancelling contract ID: {}", contractId);
        
        // Find contract
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException(contractId));
        
        // Validate contract can be cancelled
        if (contract.getStatus() == Contract.ContractStatus.CANCELLED) {
            throw new BusinessRuleException("Contract is already cancelled");
        }
        
        if (contract.getStatus().isTerminated()) {
            throw new BusinessRuleException("Cannot cancel terminated contract");
        }
        
        // Validate effective date
        if (request.getEffectiveDate() != null && request.getEffectiveDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Cancellation effective date cannot be in the future");
        }
        
        // Get cancelling user
        String cancelledBy = getCurrentUsername();
        User cancellingUser = userRepository.findByUsername(cancelledBy)
            .orElse(null);
        
        // Update contract
        contract.setStatus(Contract.ContractStatus.CANCELLED);
        contract.setCancellationReason(request.getCancellationReason());
        contract.setCancellationDate(LocalDateTime.now());
        contract.setCancelledBy(cancellingUser);
        contract.setStatusChangedAt(LocalDateTime.now());
        contract.setStatusChangedBy(cancelledBy);
        contract.setStatusChangeReason(request.getCancellationReason());
        
        contract = contractRepository.save(contract);
        
        // Get current user ID
        String username = getCurrentUsername();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        // Publish cancellation event
        eventPublisher.publishEvent(new ContractCancelledEvent(
            this, contract, request.getCancellationReason(), request.isCancelUnpaidDues(), userId));
        
        // Audit log
        auditService.logSuccess(
            AuditLog.AuditAction.CONTRACT_CANCELLED,
            "Contract",
            contract.getId(),
            String.format("Cancelled contract for flat %s. Reason: %s", 
                contract.getFlat().getFlatNumber(), request.getCancellationReason())
        );
        
        return contractMapper.toResponse(contract);
    }

    @Override
    public ContractResponse modifyContract(Long contractId, ContractModificationRequest request) {
        log.info("Modifying contract ID: {}", contractId);
        
        // Find contract
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException(contractId));
        
        // Validate contract can be modified
        if (!contract.isModifiable()) {
            throw new BusinessRuleException("Contract cannot be modified after dues are generated");
        }
        
        // Create modified contract
        Contract modifiedContract = Contract.builder()
            .flat(contract.getFlat())
            .tenant(contract.getTenant())
            .tenantName(contract.getTenantName())
            .tenantContact(contract.getTenantContact())
            .tenantEmail(contract.getTenantEmail())
            .startDate(contract.getStartDate())
            .endDate(request.getEndDate() != null ? request.getEndDate() : contract.getEndDate())
            .monthlyRent(request.getNewMonthlyRent() != null ? 
                request.getNewMonthlyRent() : contract.getMonthlyRent())
            .dayOfMonth(request.getNewDayOfMonth() != null ? 
                request.getNewDayOfMonth() : contract.getDayOfMonth())
            .depositAmount(contract.getDepositAmount())
            .autoRenew(contract.isAutoRenew())
            .status(Contract.ContractStatus.ACTIVE)
            .previousContract(contract)
            .notes(request.getNotes())
            .build();
        
        // Save modified contract
        modifiedContract = contractRepository.save(modifiedContract);
        
        // Update original contract
        contract.setStatus(Contract.ContractStatus.SUPERSEDED);
        contract.setStatusChangedAt(LocalDateTime.now());
        contract.setStatusChangedBy(getCurrentUsername());
        contract.setStatusChangeReason("Superseded by modified contract ID: " + modifiedContract.getId());
        contractRepository.save(contract);
        
        // Get current user ID
        String username = getCurrentUsername();
        User currentUser = userRepository.findByUsername(username).orElse(null);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        // Publish modification event
        eventPublisher.publishEvent(new ContractModifiedEvent(
            this, contract, modifiedContract, request.getEffectiveDate(), 
            request.getModificationDetails(), userId, request.isRegenerateDues()));
        
        // Audit log
        auditService.logSuccess(
            AuditLog.AuditAction.CONTRACT_MODIFIED,
            "Contract",
            modifiedContract.getId(),
            String.format("Modified contract %d with new contract %d", 
                contract.getId(), modifiedContract.getId())
        );
        
        return contractMapper.toResponse(modifiedContract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractById(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new ContractNotFoundException(contractId));
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getActiveContractByFlatId(Long flatId) {
        Contract contract = contractRepository.findByFlatIdAndStatus(flatId, Contract.ContractStatus.ACTIVE)
            .orElseThrow(() -> ContractNotFoundException.noActiveContract(flatId));
        return contractMapper.toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummaryResponse> getContractsByFlatId(Long flatId) {
        return contractRepository.findByFlatIdOrderByStartDateDesc(flatId).stream()
            .map(contractMapper::toSummaryResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> getContractsByBuildingId(Long buildingId, Pageable pageable) {
        return contractRepository.findByBuildingId(buildingId, pageable)
            .map(contractMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContractSummaryResponse> searchContractsByTenantName(String search, Pageable pageable) {
        return contractRepository.searchByTenantName(search, pageable)
            .map(contractMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummaryResponse> getExpiringContracts(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);
        return contractRepository.findExpiringContracts(today, futureDate).stream()
            .map(contractMapper::toSummaryResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummaryResponse> getContractsWithOverdueDues() {
        return contractRepository.findContractsWithOverdueDues(LocalDate.now()).stream()
            .map(contractMapper::toSummaryResponse)
            .toList();
    }

    @Override
    public void updateContractStatuses() {
        LocalDate today = LocalDate.now();
        List<Contract> contractsToUpdate = contractRepository.findContractsNeedingStatusUpdate(today);
        
        for (Contract contract : contractsToUpdate) {
            if (contract.getStatus() == Contract.ContractStatus.PENDING && !contract.getStartDate().isAfter(today)) {
                contract.setStatus(Contract.ContractStatus.ACTIVE);
                contract.setStatusChangedAt(LocalDateTime.now());
                contract.setStatusChangedBy("SYSTEM");
                contract.setStatusChangeReason("Contract activated on start date");
            } else if (contract.getStatus() == Contract.ContractStatus.ACTIVE && contract.getEndDate().isBefore(today)) {
                contract.setStatus(Contract.ContractStatus.EXPIRED);
                contract.setStatusChangedAt(LocalDateTime.now());
                contract.setStatusChangedBy("SYSTEM");
                contract.setStatusChangeReason("Contract expired");
            }
            contractRepository.save(contract);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getContractStatisticsByBuilding(Long buildingId) {
        Object[] stats = (Object[]) contractRepository.getContractStatisticsByBuilding(buildingId);
        Map<String, Object> result = new HashMap<>();
        if (stats != null) {
            result.put("total", stats[0]);
            result.put("active", stats[1]);
            result.put("expired", stats[2]);
            result.put("pending", stats[3]);
        }
        return result;
    }

    @Override
    public boolean validateContractDates(Long flatId, LocalDate startDate, LocalDate endDate, Long excludeContractId) {
        List<Contract> overlapping = contractRepository.findOverlappingContracts(
            flatId, startDate, endDate, excludeContractId != null ? excludeContractId : 0L);
        return overlapping.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractSummaryResponse> getRenewableContracts(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today;
        LocalDate endDate = today.plusDays(daysAhead);
        return contractRepository.findRenewableContracts(startDate, endDate, today).stream()
            .map(contractMapper::toSummaryResponse)
            .toList();
    }

    @Override
    public List<ContractExpiryNotification> generateExpiryNotifications() {
        // Get contracts expiring in 30 days
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        List<Contract> expiringContracts = contractRepository.findExpiringContracts(today, futureDate);
        
        return expiringContracts.stream()
            .map(contractMapper::toExpiryNotification)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalMonthlyRentByBuilding(Long buildingId) {
        BigDecimal total = contractRepository.getTotalMonthlyRentByBuilding(buildingId);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveContract(Long flatId) {
        return contractRepository.existsByFlatIdAndStatus(flatId, Contract.ContractStatus.ACTIVE);
    }

    /**
     * Validate contract dates
     */
    private void validateContractDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date must be after start date");
        }
        
        if (startDate.isBefore(LocalDate.now())) {
            throw new ValidationException("Start date cannot be in the past");
        }
    }

    /**
     * Get current authenticated username
     */
    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}