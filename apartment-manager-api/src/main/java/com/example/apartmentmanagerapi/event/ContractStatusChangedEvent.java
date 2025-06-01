package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.Contract.ContractStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a contract status changes (e.g., PENDING to ACTIVE, ACTIVE to EXPIRED)
 */
@Getter
public class ContractStatusChangedEvent extends ApplicationEvent {
    
    private final Contract contract;
    private final ContractStatus oldStatus;
    private final ContractStatus newStatus;
    private final boolean automaticChange;
    
    /**
     * Create a new ContractStatusChangedEvent
     * @param source The object on which the event initially occurred
     * @param contract The contract whose status changed
     * @param oldStatus Previous status
     * @param newStatus New status
     * @param automaticChange Whether this was an automatic change (scheduled task) or manual
     */
    public ContractStatusChangedEvent(Object source, Contract contract,
                                     ContractStatus oldStatus, ContractStatus newStatus,
                                     boolean automaticChange) {
        super(source);
        this.contract = contract;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.automaticChange = automaticChange;
    }
}