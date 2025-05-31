package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserBuildingAssignment entity represents the many-to-many relationship
 * between users and apartment buildings.
 * 
 * This join table allows managers to be assigned to multiple buildings,
 * and buildings to have multiple managers. The unique constraint ensures
 * that a user can only be assigned once to a specific building.
 */
@Entity
@Table(name = "user_building_assignments",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "building_id"},
            name = "uk_user_building")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBuildingAssignment {
    
    /**
     * Unique identifier for the assignment
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user being assigned to a building
     * Typically a user with MANAGER role
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * The building being assigned to the user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private ApartmentBuilding building;
    
    /**
     * Date when this assignment was made
     * Helps track assignment history
     */
    @Column(name = "assigned_date", nullable = false)
    @Builder.Default
    private LocalDateTime assignedDate = LocalDateTime.now();
    
    /**
     * Optional date when this assignment ends
     * Null means the assignment is still active
     */
    @Column(name = "unassigned_date")
    private LocalDateTime unassignedDate;
    
    /**
     * User who created this assignment (typically an ADMIN)
     * For audit trail purposes
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_user_id")
    private User assignedBy;
    
    /**
     * Notes about this assignment (optional)
     * Can include special permissions or responsibilities
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Whether this assignment is currently active
     * Computed based on unassignedDate being null
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    /**
     * Timestamp when this record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (assignedDate == null) {
            assignedDate = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
    
    /**
     * Automatically update the timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update isActive based on unassignedDate
        isActive = (unassignedDate == null);
    }
    
    /**
     * Helper method to check if assignment is currently active
     * @return true if assignment has no end date
     */
    public boolean isCurrentlyActive() {
        return unassignedDate == null;
    }
    
    /**
     * Helper method to deactivate this assignment
     * Sets the unassigned date to now and marks as inactive
     */
    public void deactivate() {
        this.unassignedDate = LocalDateTime.now();
        this.isActive = false;
    }
}