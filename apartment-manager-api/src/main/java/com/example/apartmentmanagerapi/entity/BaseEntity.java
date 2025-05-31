package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity class that provides common fields for all entities.
 * Includes audit fields (created/updated timestamps and users) and optimistic locking.
 * 
 * @param <ID> The type of the entity's identifier
 */
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<ID extends Serializable> implements Serializable {
    
    /**
     * Timestamp when the entity was created.
     * Automatically populated by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Username of the user who created this entity.
     * Automatically populated by JPA auditing.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    /**
     * Timestamp when the entity was last updated.
     * Automatically populated by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Username of the user who last updated this entity.
     * Automatically populated by JPA auditing.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
    
    /**
     * Version field for optimistic locking.
     * JPA will automatically increment this value on each update.
     * If the version in the database doesn't match the version in the entity
     * being updated, an OptimisticLockException will be thrown.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    /**
     * Get the ID of this entity.
     * Subclasses must implement this to return their specific ID.
     * 
     * @return The entity's ID
     */
    public abstract ID getId();
    
    /**
     * Set the ID of this entity.
     * Subclasses must implement this to set their specific ID.
     * 
     * @param id The entity's ID
     */
    public abstract void setId(ID id);
    
    /**
     * Callback method called before the entity is persisted.
     * Can be overridden by subclasses to perform pre-persist logic.
     */
    @PrePersist
    protected void onCreate() {
        // Subclasses can override this method to add custom pre-persist logic
    }
    
    /**
     * Callback method called before the entity is updated.
     * Can be overridden by subclasses to perform pre-update logic.
     */
    @PreUpdate
    protected void onUpdate() {
        // Subclasses can override this method to add custom pre-update logic
    }
    
    /**
     * Check if this is a new entity (not yet persisted).
     * 
     * @return true if the entity is new, false otherwise
     */
    @Transient
    public boolean isNew() {
        return getId() == null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        BaseEntity<?> that = (BaseEntity<?>) o;
        
        // Use ID for equality if both entities have IDs
        if (getId() != null && that.getId() != null) {
            return getId().equals(that.getId());
        }
        
        // For new entities, use object identity
        return super.equals(o);
    }
    
    @Override
    public int hashCode() {
        // Use ID for hash code if available, otherwise use object identity
        return getId() != null ? getId().hashCode() : super.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, version=%s]", 
            getClass().getSimpleName(), getId(), getVersion());
    }
}