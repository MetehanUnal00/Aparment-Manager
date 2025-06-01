package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a contract is renewed
 */
@Getter
public class ContractRenewedEvent extends ApplicationEvent {
    
    private final Contract oldContract;
    private final Contract newContract;
    private final boolean generateDuesImmediately;
    private final Long renewedByUserId;
    
    /**
     * Create a new ContractRenewedEvent
     * @param source The object on which the event initially occurred
     * @param oldContract The previous contract
     * @param newContract The new renewed contract
     * @param generateDuesImmediately Whether to generate dues immediately
     * @param renewedByUserId ID of the user who renewed the contract
     */
    public ContractRenewedEvent(Object source, Contract oldContract, 
                               Contract newContract, boolean generateDuesImmediately,
                               Long renewedByUserId) {
        super(source);
        this.oldContract = oldContract;
        this.newContract = newContract;
        this.generateDuesImmediately = generateDuesImmediately;
        this.renewedByUserId = renewedByUserId;
    }
}