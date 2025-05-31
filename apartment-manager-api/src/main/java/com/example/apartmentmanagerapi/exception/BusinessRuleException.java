package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a business rule is violated.
 * This is for domain-specific business logic violations that don't fit other categories.
 * This typically maps to HTTP 422 Unprocessable Entity status code.
 */
public class BusinessRuleException extends ApartmentManagerException {
    
    /**
     * The business rule that was violated
     */
    private final String rule;
    
    /**
     * Additional context about the violation
     */
    private final Object context;
    
    /**
     * Constructor for simple business rule violation
     * @param message The error message describing the violation
     */
    public BusinessRuleException(String message) {
        super(message);
        this.rule = null;
        this.context = null;
    }
    
    /**
     * Constructor with rule specification
     * @param rule The business rule that was violated
     * @param message The error message
     */
    public BusinessRuleException(String rule, String message) {
        super(message);
        this.rule = rule;
        this.context = null;
    }
    
    /**
     * Constructor with rule and context
     * @param rule The business rule that was violated
     * @param message The error message
     * @param context Additional context (e.g., the values that caused the violation)
     */
    public BusinessRuleException(String rule, String message, Object context) {
        super(message);
        this.rule = rule;
        this.context = context;
    }
    
    /**
     * Get the business rule that was violated
     * @return The rule
     */
    public String getRule() {
        return rule;
    }
    
    /**
     * Get additional context about the violation
     * @return The context object
     */
    public Object getContext() {
        return context;
    }
}