package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.Expense;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Event published when an expense is recorded for a building.
 * This event can be used to trigger related processes like:
 * - Distributing expense to flats if needed
 * - Updating building financial statistics
 * - Notifying building managers about significant expenses
 */
@Getter
public class ExpenseRecordedEvent extends ApplicationEvent {
    
    private final Long expenseId;
    private final Long buildingId;
    private final BigDecimal amount;
    private final Expense.ExpenseCategory category;
    private final LocalDate expenseDate;
    private final boolean isRecurring;
    private final boolean shouldDistributeToFlats;
    
    /**
     * Creates a new ExpenseRecordedEvent.
     * 
     * @param source the object on which the event initially occurred
     * @param expenseId the ID of the recorded expense
     * @param buildingId the ID of the building
     * @param amount the expense amount
     * @param category the expense category
     * @param expenseDate the date of the expense
     * @param isRecurring whether this is a recurring expense
     * @param shouldDistributeToFlats whether to distribute to flats
     */
    public ExpenseRecordedEvent(Object source, Long expenseId, Long buildingId,
                               BigDecimal amount, Expense.ExpenseCategory category,
                               LocalDate expenseDate, boolean isRecurring,
                               boolean shouldDistributeToFlats) {
        super(source);
        this.expenseId = expenseId;
        this.buildingId = buildingId;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.isRecurring = isRecurring;
        this.shouldDistributeToFlats = shouldDistributeToFlats;
    }
}