package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new contract is created
 */
@Getter
public class ContractCreatedEvent extends ApplicationEvent {
    
    private final Contract contract;
    private final boolean generateDuesImmediately;
    private final Long createdByUserId;
    
    /**
     * Create a new ContractCreatedEvent
     * @param source The object on which the event initially occurred
     * @param contract The created contract
     * @param generateDuesImmediately Whether to generate dues immediately
     * @param createdByUserId ID of the user who created the contract
     */
    public ContractCreatedEvent(Object source, Contract contract, 
                               boolean generateDuesImmediately, 
                               Long createdByUserId) {
        super(source);
        this.contract = contract;
        this.generateDuesImmediately = generateDuesImmediately;
        this.createdByUserId = createdByUserId;
    }
}