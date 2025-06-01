package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.AuditLog;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.MonthlyDuesGeneratedEvent;
import com.example.apartmentmanagerapi.exception.BusinessRuleException;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for contract due generation
 * Handles automatic generation of monthly dues based on contract terms
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractDueGenerationService implements IContractDueGenerationService {

    private final MonthlyDueRepository monthlyDueRepository;
    private final ContractRepository contractRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final IAuditService auditService;

    @Override
    public List<MonthlyDue> generateDuesForContract(Contract contract) {
        log.info("Generating monthly dues for contract ID: {}", contract.getId());
        
        // Validate contract hasn't already generated dues
        if (contract.isDuesGenerated()) {
            throw new BusinessRuleException("Dues already generated for this contract");
        }
        
        List<MonthlyDue> generatedDues = new ArrayList<>();
        LocalDate currentDueDate = calculateFirstDueDate(contract);
        
        // Generate dues for each month in the contract period
        while (!currentDueDate.isAfter(contract.getEndDate())) {
            MonthlyDue monthlyDue = createMonthlyDue(contract, currentDueDate);
            generatedDues.add(monthlyDue);
            
            // Move to next month
            currentDueDate = calculateNextDueDate(currentDueDate, contract.getDayOfMonth());
            
            // Break if we've gone past the contract end date
            if (currentDueDate.isAfter(contract.getEndDate())) {
                break;
            }
        }
        
        // Save all dues
        generatedDues = monthlyDueRepository.saveAll(generatedDues);
        
        // Mark contract as dues generated
        contract.setDuesGenerated(true);
        contractRepository.save(contract);
        
        // Publish event for generated dues
        if (!generatedDues.isEmpty()) {
            LocalDate firstDueDate = generatedDues.get(0).getDueDate();
            eventPublisher.publishEvent(new MonthlyDuesGeneratedEvent(
                this,
                contract.getFlat().getApartmentBuilding().getId(),
                firstDueDate.getYear(),
                firstDueDate.getMonthValue(),
                1, // One flat affected (contract is for one flat)
                firstDueDate
            ));
        }
        
        log.info("Generated {} monthly dues for contract ID: {}", 
            generatedDues.size(), contract.getId());
        
        return generatedDues;
    }

    @Override
    public List<MonthlyDue> generateDuesForContractExtension(Contract contract, LocalDate extensionStartDate) {
        log.info("Generating extension dues for contract ID: {} starting from {}", 
            contract.getId(), extensionStartDate);
        
        List<MonthlyDue> generatedDues = new ArrayList<>();
        LocalDate currentDueDate = adjustDayOfMonth(extensionStartDate, contract.getDayOfMonth());
        
        // Generate dues from extension start to contract end
        while (!currentDueDate.isAfter(contract.getEndDate())) {
            MonthlyDue monthlyDue = createMonthlyDue(contract, currentDueDate);
            monthlyDue.setDescription(monthlyDue.getDescription() + " (Extension)");
            generatedDues.add(monthlyDue);
            
            // Move to next month
            currentDueDate = calculateNextDueDate(currentDueDate, contract.getDayOfMonth());
        }
        
        // Save all dues
        generatedDues = monthlyDueRepository.saveAll(generatedDues);
        
        log.info("Generated {} extension dues for contract ID: {}", 
            generatedDues.size(), contract.getId());
        
        return generatedDues;
    }

    @Override
    public int cancelUnpaidDuesForContract(Contract contract) {
        log.info("Cancelling unpaid dues for contract ID: {}", contract.getId());
        
        // Find all unpaid dues for the contract
        List<MonthlyDue> unpaidDues = monthlyDueRepository.findByContractId(contract.getId())
            .stream()
            .filter(due -> !due.isPaid())
            .filter(due -> due.getPaymentStatus() != MonthlyDue.PaymentStatus.CANCELLED)
            .collect(Collectors.toList());
        
        // Cancel each unpaid due
        unpaidDues.forEach(due -> {
            due.setPaymentStatus(MonthlyDue.PaymentStatus.CANCELLED);
            due.setDescription(due.getDescription() + " - Cancelled due to contract cancellation");
        });
        
        // Save cancelled dues
        if (!unpaidDues.isEmpty()) {
            monthlyDueRepository.saveAll(unpaidDues);
        }
        
        log.info("Cancelled {} unpaid dues for contract ID: {}", 
            unpaidDues.size(), contract.getId());
        
        return unpaidDues.size();
    }

    @Override
    public void regenerateDuesForModifiedContract(Contract oldContract, Contract newContract, LocalDate effectiveDate) {
        log.info("Regenerating dues for modified contract. Old: {}, New: {}, Effective: {}", 
            oldContract.getId(), newContract.getId(), effectiveDate);
        
        // Find existing dues that need to be updated
        List<MonthlyDue> existingDues = monthlyDueRepository.findByContractId(oldContract.getId());
        
        // Separate paid and unpaid dues
        List<MonthlyDue> unpaidFutureDues = existingDues.stream()
            .filter(due -> !due.isPaid())
            .filter(due -> !due.getDueDate().isBefore(effectiveDate))
            .collect(Collectors.toList());
        
        if (!unpaidFutureDues.isEmpty()) {
            // Delete unpaid future dues
            monthlyDueRepository.deleteAll(unpaidFutureDues);
            
            // Generate new dues with updated terms
            LocalDate startDate = unpaidFutureDues.get(0).getDueDate();
            List<MonthlyDue> newDues = new ArrayList<>();
            LocalDate currentDueDate = adjustDayOfMonth(startDate, newContract.getDayOfMonth());
            
            while (!currentDueDate.isAfter(newContract.getEndDate())) {
                MonthlyDue monthlyDue = createMonthlyDue(newContract, currentDueDate);
                monthlyDue.setDescription(monthlyDue.getDescription() + " (Modified)");
                newDues.add(monthlyDue);
                
                currentDueDate = calculateNextDueDate(currentDueDate, newContract.getDayOfMonth());
            }
            
            // Save new dues
            monthlyDueRepository.saveAll(newDues);
            
            log.info("Deleted {} old dues and created {} new dues for modified contract", 
                unpaidFutureDues.size(), newDues.size());
        }
        
        // Mark new contract as dues generated
        newContract.setDuesGenerated(true);
        contractRepository.save(newContract);
    }

    /**
     * Preview dues for a contract without saving
     * Not part of the interface but useful for internal operations
     */
    public List<MonthlyDue> previewDuesForContract(Contract contract) {
        log.info("Previewing dues for contract ID: {}", contract.getId());
        
        List<MonthlyDue> previewDues = new ArrayList<>();
        LocalDate currentDueDate = calculateFirstDueDate(contract);
        
        while (!currentDueDate.isAfter(contract.getEndDate())) {
            MonthlyDue monthlyDue = createMonthlyDue(contract, currentDueDate);
            previewDues.add(monthlyDue);
            
            currentDueDate = calculateNextDueDate(currentDueDate, contract.getDayOfMonth());
        }
        
        return previewDues;
    }

    @Override
    public List<LocalDate> calculateDueDates(LocalDate startDate, LocalDate endDate, int dayOfMonth) {
        log.info("Calculating due dates from {} to {} for day {}", startDate, endDate, dayOfMonth);
        
        List<LocalDate> dueDates = new ArrayList<>();
        LocalDate currentDueDate = calculateFirstDueDateFromStartDate(startDate, dayOfMonth);
        
        while (!currentDueDate.isAfter(endDate)) {
            dueDates.add(currentDueDate);
            currentDueDate = calculateNextDueDate(currentDueDate, dayOfMonth);
        }
        
        return dueDates;
    }

    @Override
    public LocalDate adjustDueDateForMonth(LocalDate baseDate, int dayOfMonth) {
        return adjustDayOfMonth(baseDate, dayOfMonth);
    }

    @Override
    public boolean duesExistForContract(Contract contract) {
        log.info("Checking if dues exist for contract ID: {}", contract.getId());
        
        // Check both the contract flag and actual dues in database
        if (contract.isDuesGenerated()) {
            return true;
        }
        
        // Double-check in database
        List<MonthlyDue> existingDues = monthlyDueRepository.findByContractAndStatusIn(
            contract, 
            List.of(MonthlyDue.DueStatus.UNPAID, MonthlyDue.DueStatus.PAID, MonthlyDue.DueStatus.PARTIALLY_PAID)
        );
        
        return !existingDues.isEmpty();
    }

    @Override
    public List<MonthlyDue> getDuesByContractId(Long contractId) {
        log.info("Getting dues for contract ID: {}", contractId);
        
        // Find contract and get its dues
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new BusinessRuleException("Contract not found with ID: " + contractId));
            
        return monthlyDueRepository.findByContract(contract);
    }

    @Override
    public boolean validateDueGenerationParameters(Contract contract) {
        log.info("Validating due generation parameters for contract ID: {}", contract.getId());
        
        // Validate contract is active
        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            log.warn("Contract {} is not active. Status: {}", contract.getId(), contract.getStatus());
            return false;
        }
        
        // Validate dates
        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            log.warn("Contract {} has null dates", contract.getId());
            return false;
        }
        
        // Validate day of month
        if (contract.getDayOfMonth() < 1 || contract.getDayOfMonth() > 31) {
            log.warn("Contract {} has invalid day of month: {}", contract.getId(), contract.getDayOfMonth());
            return false;
        }
        
        // Validate monthly rent
        if (contract.getMonthlyRent() == null || contract.getMonthlyRent().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Contract {} has invalid monthly rent: {}", contract.getId(), contract.getMonthlyRent());
            return false;
        }
        
        // Log validation success and trigger audit
        auditService.logSuccess(
            AuditLog.AuditAction.CONTRACT_DUES_GENERATED,
            "Contract",
            contract.getId(),
            String.format("Contract %d validated for due generation", contract.getId())
        );
        
        return true;
    }

    /**
     * Calculate the first due date for a contract
     */
    private LocalDate calculateFirstDueDate(Contract contract) {
        return calculateFirstDueDateFromStartDate(contract.getStartDate(), contract.getDayOfMonth());
    }
    
    /**
     * Calculate the first due date from a start date and day of month
     */
    private LocalDate calculateFirstDueDateFromStartDate(LocalDate startDate, int dayOfMonth) {
        // If contract starts on or before the due day, first due is in the same month
        if (startDate.getDayOfMonth() <= dayOfMonth) {
            return adjustDayOfMonth(startDate, dayOfMonth);
        } else {
            // Otherwise, first due is next month
            return adjustDayOfMonth(startDate.plusMonths(1), dayOfMonth);
        }
    }

    /**
     * Calculate next due date based on current due date and day of month
     */
    private LocalDate calculateNextDueDate(LocalDate currentDueDate, int dayOfMonth) {
        LocalDate nextMonth = currentDueDate.plusMonths(1);
        return adjustDayOfMonth(nextMonth, dayOfMonth);
    }

    /**
     * Adjust day of month for months with fewer days (e.g., February)
     */
    private LocalDate adjustDayOfMonth(LocalDate date, int dayOfMonth) {
        int lastDayOfMonth = date.lengthOfMonth();
        int adjustedDay = Math.min(dayOfMonth, lastDayOfMonth);
        return date.withDayOfMonth(adjustedDay);
    }

    /**
     * Create a monthly due entity
     */
    private MonthlyDue createMonthlyDue(Contract contract, LocalDate dueDate) {
        return MonthlyDue.builder()
            .flat(contract.getFlat())
            .contract(contract)
            .dueAmount(contract.getMonthlyRent())
            .baseRent(contract.getMonthlyRent())
            .additionalCharges(BigDecimal.ZERO)
            .dueDate(dueDate)
            .status(MonthlyDue.DueStatus.UNPAID)
            .paymentStatus(MonthlyDue.PaymentStatus.PENDING)
            .dueDescription(String.format("Monthly rent for %s %d", 
                dueDate.getMonth().toString(), dueDate.getYear()))
            .description(String.format("Contract #%d - Monthly rent for %s", 
                contract.getId(), dueDate.getMonth().toString() + " " + dueDate.getYear()))
            .build();
    }
}