package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.validation.ValidDateRange;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for date range requests used in various API endpoints.
 * Ensures that the start date is before or equal to the end date.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidDateRange(startDate = "startDate", endDate = "endDate", 
               message = "Start date must be before or equal to end date")
public class DateRangeRequest {
    
    /**
     * The start date of the range.
     */
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    /**
     * The end date of the range.
     */
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    /**
     * Creates a date range for a specific month.
     * 
     * @param year the year
     * @param month the month (1-12)
     * @return DateRangeRequest for the entire month
     */
    public static DateRangeRequest forMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return new DateRangeRequest(start, end);
    }
    
    /**
     * Creates a date range for a specific year.
     * 
     * @param year the year
     * @return DateRangeRequest for the entire year
     */
    public static DateRangeRequest forYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return new DateRangeRequest(start, end);
    }
    
    /**
     * Creates a date range for the last N days.
     * 
     * @param days number of days to go back
     * @return DateRangeRequest for the last N days
     */
    public static DateRangeRequest forLastDays(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        return new DateRangeRequest(start, end);
    }
}