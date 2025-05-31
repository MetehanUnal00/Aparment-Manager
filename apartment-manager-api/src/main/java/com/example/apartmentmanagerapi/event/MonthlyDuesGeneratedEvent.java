package com.example.apartmentmanagerapi.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * Event published when monthly dues are generated for a building.
 * This event can be used to trigger related processes like:
 * - Sending monthly due notifications to tenants
 * - Updating building financial reports
 * - Triggering automated reminders
 */
@Getter
public class MonthlyDuesGeneratedEvent extends ApplicationEvent {
    
    private final Long buildingId;
    private final int year;
    private final int month;
    private final int numberOfFlatsAffected;
    private final LocalDate dueDate;
    
    /**
     * Creates a new MonthlyDuesGeneratedEvent.
     * 
     * @param source the object on which the event initially occurred
     * @param buildingId the ID of the building
     * @param year the year for which dues were generated
     * @param month the month for which dues were generated
     * @param numberOfFlatsAffected number of flats that had dues generated
     * @param dueDate the due date for the generated dues
     */
    public MonthlyDuesGeneratedEvent(Object source, Long buildingId, int year, int month,
                                    int numberOfFlatsAffected, LocalDate dueDate) {
        super(source);
        this.buildingId = buildingId;
        this.year = year;
        this.month = month;
        this.numberOfFlatsAffected = numberOfFlatsAffected;
        this.dueDate = dueDate;
    }
}