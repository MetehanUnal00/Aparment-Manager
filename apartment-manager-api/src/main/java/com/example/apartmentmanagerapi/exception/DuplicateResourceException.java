package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * This typically maps to HTTP 409 Conflict status code.
 */
public class DuplicateResourceException extends ApartmentManagerException {
    
    /**
     * The type of resource that already exists
     */
    private final String resourceType;
    
    /**
     * The field that contains the duplicate value
     */
    private final String fieldName;
    
    /**
     * The duplicate value
     */
    private final Object fieldValue;
    
    /**
     * Constructor for duplicate resource
     * @param resourceType The type of resource (e.g., "User", "Flat")
     * @param fieldName The field with duplicate value (e.g., "username", "flatNumber")
     * @param fieldValue The duplicate value
     */
    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceType, fieldName, fieldValue));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    /**
     * Constructor with custom message
     * @param message Custom error message
     * @param resourceType The type of resource
     * @param fieldName The field with duplicate value
     * @param fieldValue The duplicate value
     */
    public DuplicateResourceException(String message, String resourceType, String fieldName, Object fieldValue) {
        super(message);
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    /**
     * Constructor for composite duplicate (e.g., flat number in a specific building)
     * @param resourceType The type of resource
     * @param fieldName The field with duplicate value
     * @param fieldValue The duplicate value
     * @param context Additional context (e.g., "in building X")
     */
    public DuplicateResourceException(String resourceType, String fieldName, Object fieldValue, String context) {
        super(String.format("%s already exists with %s: %s %s", resourceType, fieldName, fieldValue, context));
        this.resourceType = resourceType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    /**
     * Get the type of resource that already exists
     * @return The resource type
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Get the field name that contains the duplicate value
     * @return The field name
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Get the duplicate value
     * @return The field value
     */
    public Object getFieldValue() {
        return fieldValue;
    }
}