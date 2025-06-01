package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a contract is cancelled
 */
@Getter
public class ContractCancelledEvent extends ApplicationEvent {
    
    private final Contract contract;
    private final String cancellationReason;
    private final boolean cancelUnpaidDues;
    private final Long cancelledByUserId;
    
    /**
     * Create a new ContractCancelledEvent
     * @param source The object on which the event initially occurred
     * @param contract The cancelled contract
     * @param cancellationReason Reason for cancellation
     * @param cancelUnpaidDues Whether to cancel unpaid dues
     * @param cancelledByUserId ID of the user who cancelled the contract
     */
    public ContractCancelledEvent(Object source, Contract contract, 
                                 String cancellationReason, boolean cancelUnpaidDues,
                                 Long cancelledByUserId) {
        super(source);
        this.contract = contract;
        this.cancellationReason = cancellationReason;
        this.cancelUnpaidDues = cancelUnpaidDues;
        this.cancelledByUserId = cancelledByUserId;
    }
}