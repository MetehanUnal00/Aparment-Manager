package com.example.apartmentmanagerapi.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a payment is successfully recorded in the system.
 * This event can be used to trigger related processes like:
 * - Sending payment confirmation emails
 * - Updating flat balance calculations
 * - Triggering financial reports
 * - Notifying building managers
 */
@Getter
public class PaymentRecordedEvent extends ApplicationEvent {
    
    private final Long paymentId;
    private final Long flatId;
    private final Long buildingId;
    private final BigDecimal amount;
    private final LocalDateTime paymentDate;
    private final String tenantName;
    private final String tenantEmail;
    
    /**
     * Creates a new PaymentRecordedEvent.
     * 
     * @param source the object on which the event initially occurred
     * @param paymentId the ID of the recorded payment
     * @param flatId the ID of the flat
     * @param buildingId the ID of the building
     * @param amount the payment amount
     * @param paymentDate the date of payment
     * @param tenantName the tenant's name (may be null)
     * @param tenantEmail the tenant's email (may be null)
     */
    public PaymentRecordedEvent(Object source, Long paymentId, Long flatId, Long buildingId,
                               BigDecimal amount, LocalDateTime paymentDate, 
                               String tenantName, String tenantEmail) {
        super(source);
        this.paymentId = paymentId;
        this.flatId = flatId;
        this.buildingId = buildingId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.tenantName = tenantName;
        this.tenantEmail = tenantEmail;
    }
}