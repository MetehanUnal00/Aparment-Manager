package com.example.apartmentmanagerapi.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new flat is created in the system.
 * This event can be used to trigger related processes like:
 * - Sending welcome emails to tenants
 * - Creating initial monthly dues
 * - Updating building statistics
 */
@Getter
public class FlatCreatedEvent extends ApplicationEvent {
    
    private final Long flatId;
    private final Long buildingId;
    private final String flatNumber;
    private final String tenantEmail;
    private final String tenantName;
    
    /**
     * Creates a new FlatCreatedEvent.
     * 
     * @param source the object on which the event initially occurred
     * @param flatId the ID of the newly created flat
     * @param buildingId the ID of the building containing the flat
     * @param flatNumber the flat number
     * @param tenantEmail the tenant's email (may be null)
     * @param tenantName the tenant's name (may be null)
     */
    public FlatCreatedEvent(Object source, Long flatId, Long buildingId, 
                           String flatNumber, String tenantEmail, String tenantName) {
        super(source);
        this.flatId = flatId;
        this.buildingId = buildingId;
        this.flatNumber = flatNumber;
        this.tenantEmail = tenantEmail;
        this.tenantName = tenantName;
    }
}