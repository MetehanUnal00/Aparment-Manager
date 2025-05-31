package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.UserBuildingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserBuildingAssignment entity.
 * Manages the many-to-many relationship between users and buildings,
 * supporting role-based access control queries.
 */
@Repository
public interface UserBuildingAssignmentRepository extends JpaRepository<UserBuildingAssignment, Long> {
    
    /**
     * Find assignment by user and building
     * @param userId The user ID
     * @param buildingId The building ID
     * @return Optional containing the assignment if found
     */
    Optional<UserBuildingAssignment> findByUserIdAndBuildingId(Long userId, Long buildingId);
    
    /**
     * Check if an active assignment exists
     * @param userId The user ID
     * @param buildingId The building ID
     * @return true if an active assignment exists
     */
    boolean existsByUserIdAndBuildingIdAndIsActiveTrue(Long userId, Long buildingId);
    
    /**
     * Find all active assignments for a user
     * @param userId The user ID
     * @return List of active assignments
     */
    List<UserBuildingAssignment> findByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Find all active assignments for a building
     * @param buildingId The building ID
     * @return List of active assignments
     */
    List<UserBuildingAssignment> findByBuildingIdAndIsActiveTrue(Long buildingId);
    
    /**
     * Find all assignments (active and inactive) for a user
     * @param userId The user ID
     * @return List of all assignments
     */
    List<UserBuildingAssignment> findByUserId(Long userId);
    
    /**
     * Find all assignments (active and inactive) for a building
     * @param buildingId The building ID
     * @return List of all assignments
     */
    List<UserBuildingAssignment> findByBuildingId(Long buildingId);
    
    /**
     * Get all buildings managed by a user
     * @param userId The user ID
     * @return List of building IDs
     */
    @Query("SELECT uba.building.id FROM UserBuildingAssignment uba " +
           "WHERE uba.user.id = :userId AND uba.isActive = true")
    List<Long> findActiveBuildingIdsByUserId(@Param("userId") Long userId);
    
    /**
     * Get all users managing a building
     * @param buildingId The building ID
     * @return List of user IDs
     */
    @Query("SELECT uba.user.id FROM UserBuildingAssignment uba " +
           "WHERE uba.building.id = :buildingId AND uba.isActive = true")
    List<Long> findActiveUserIdsByBuildingId(@Param("buildingId") Long buildingId);
    
    /**
     * Count active assignments for a user
     * @param userId The user ID
     * @return Number of active assignments
     */
    long countByUserIdAndIsActiveTrue(Long userId);
    
    /**
     * Count active assignments for a building
     * @param buildingId The building ID
     * @return Number of active assignments
     */
    long countByBuildingIdAndIsActiveTrue(Long buildingId);
    
    /**
     * Find assignments made by a specific user
     * @param assignedByUserId The ID of the user who made the assignments
     * @return List of assignments
     */
    List<UserBuildingAssignment> findByAssignedById(Long assignedByUserId);
    
    /**
     * Deactivate all assignments for a user
     * Used when a user is deactivated or role changed
     * @param userId The user ID
     * @return Number of records updated
     */
    @Query("UPDATE UserBuildingAssignment uba " +
           "SET uba.isActive = false, uba.unassignedDate = CURRENT_TIMESTAMP " +
           "WHERE uba.user.id = :userId AND uba.isActive = true")
    int deactivateAllAssignmentsForUser(@Param("userId") Long userId);
    
    /**
     * Deactivate all assignments for a building
     * Used when a building is deactivated
     * @param buildingId The building ID
     * @return Number of records updated
     */
    @Query("UPDATE UserBuildingAssignment uba " +
           "SET uba.isActive = false, uba.unassignedDate = CURRENT_TIMESTAMP " +
           "WHERE uba.building.id = :buildingId AND uba.isActive = true")
    int deactivateAllAssignmentsForBuilding(@Param("buildingId") Long buildingId);
}