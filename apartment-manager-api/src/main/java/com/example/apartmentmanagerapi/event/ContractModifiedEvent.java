package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * Event published when a contract is modified (superseded)
 */
@Getter
public class ContractModifiedEvent extends ApplicationEvent {
    
    private final Contract oldContract;
    private final Contract newContract;
    private final LocalDate effectiveDate;
    private final String modificationReason;
    private final Long modifiedByUserId;
    private final boolean regenerateDues;
    
    /**
     * Create a new ContractModifiedEvent
     * @param source The object on which the event initially occurred
     * @param oldContract The contract being superseded
     * @param newContract The new contract
     * @param effectiveDate Effective date of modification
     * @param modificationReason Reason for modification
     * @param modifiedByUserId ID of the user who modified the contract
     * @param regenerateDues Whether to regenerate monthly dues
     */
    public ContractModifiedEvent(Object source, Contract oldContract, 
                                Contract newContract, LocalDate effectiveDate,
                                String modificationReason, Long modifiedByUserId,
                                boolean regenerateDues) {
        super(source);
        this.oldContract = oldContract;
        this.newContract = newContract;
        this.effectiveDate = effectiveDate;
        this.modificationReason = modificationReason;
        this.modifiedByUserId = modifiedByUserId;
        this.regenerateDues = regenerateDues;
    }
}