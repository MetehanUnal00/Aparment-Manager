package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 * This typically maps to HTTP 404 status code.
 */
public class ResourceNotFoundException extends ApartmentManagerException {
    
    /**
     * The type of resource that was not found
     */
    private final String resourceType;
    
    /**
     * The identifier of the resource that was not found
     */
    private final Object resourceId;
    
    /**
     * Constructor for simple resource not found
     * @param resourceType The type of resource (e.g., "Flat", "Building")
     * @param resourceId The identifier of the resource
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * Constructor with custom message
     * @param message Custom error message
     * @param resourceType The type of resource
     * @param resourceId The identifier of the resource
     */
    public ResourceNotFoundException(String message, String resourceType, Object resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * Constructor for composite resource not found (e.g., flat in a specific building)
     * @param resourceType The type of resource
     * @param resourceId The identifier of the resource
     * @param parentType The parent resource type
     * @param parentId The parent resource identifier
     */
    public ResourceNotFoundException(String resourceType, Object resourceId, String parentType, Object parentId) {
        super(String.format("%s not found with id: %s in %s: %s", resourceType, resourceId, parentType, parentId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    /**
     * Get the type of resource that was not found
     * @return The resource type
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Get the identifier of the resource that was not found
     * @return The resource identifier
     */
    public Object getResourceId() {
        return resourceId;
    }
}