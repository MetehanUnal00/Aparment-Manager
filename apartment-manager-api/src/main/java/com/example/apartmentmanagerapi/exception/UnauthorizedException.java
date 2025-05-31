package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a user is not authorized to perform an action.
 * This typically maps to HTTP 403 Forbidden status code.
 */
public class UnauthorizedException extends ApartmentManagerException {
    
    /**
     * The action that was attempted
     */
    private final String action;
    
    /**
     * The resource type the action was attempted on
     */
    private final String resourceType;
    
    /**
     * The user who attempted the action (username or ID)
     */
    private final String user;
    
    /**
     * Constructor for simple unauthorized access
     * @param message The error message
     */
    public UnauthorizedException(String message) {
        super(message);
        this.action = null;
        this.resourceType = null;
        this.user = null;
    }
    
    /**
     * Constructor for action-based unauthorized access
     * @param user The user attempting the action
     * @param action The action being attempted (e.g., "create", "update", "delete")
     * @param resourceType The type of resource
     */
    public UnauthorizedException(String user, String action, String resourceType) {
        super(String.format("User '%s' is not authorized to %s %s", user, action, resourceType));
        this.user = user;
        this.action = action;
        this.resourceType = resourceType;
    }
    
    /**
     * Constructor for resource-specific unauthorized access
     * @param user The user attempting the action
     * @param action The action being attempted
     * @param resourceType The type of resource
     * @param resourceId The specific resource ID
     */
    public UnauthorizedException(String user, String action, String resourceType, Object resourceId) {
        super(String.format("User '%s' is not authorized to %s %s with id: %s", user, action, resourceType, resourceId));
        this.user = user;
        this.action = action;
        this.resourceType = resourceType;
    }
    
    /**
     * Get the action that was attempted
     * @return The action
     */
    public String getAction() {
        return action;
    }
    
    /**
     * Get the resource type the action was attempted on
     * @return The resource type
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Get the user who attempted the action
     * @return The user
     */
    public String getUser() {
        return user;
    }
}