package com.example.apartmentmanagerapi.exception;

import java.time.LocalDate;

/**
 * Exception thrown when contract dates are invalid
 */
public class InvalidContractDateException extends ValidationException {
    
    /**
     * Create exception with message
     * @param message Error message
     */
    public InvalidContractDateException(String message) {
        super(message);
    }
    
    /**
     * Create exception for invalid date range
     * @param startDate Start date
     * @param endDate End date
     */
    public InvalidContractDateException(LocalDate startDate, LocalDate endDate) {
        super(String.format("Invalid contract dates: start date %s must be before end date %s", 
                          startDate, endDate));
    }
    
    /**
     * Create exception for past start date
     * @param startDate Start date
     */
    public static InvalidContractDateException pastStartDate(LocalDate startDate) {
        return new InvalidContractDateException(
            String.format("Contract start date %s cannot be in the past", startDate)
        );
    }
    
    /**
     * Create exception for contract too short
     * @param startDate Start date
     * @param endDate End date
     * @param minDays Minimum days required
     */
    public static InvalidContractDateException contractTooShort(LocalDate startDate, 
                                                               LocalDate endDate, 
                                                               int minDays) {
        return new InvalidContractDateException(
            String.format("Contract period from %s to %s is too short. Minimum %d days required", 
                        startDate, endDate, minDays)
        );
    }
}