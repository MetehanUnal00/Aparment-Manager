package com.example.apartmentmanagerapi.entity; // Ensure this package is correct

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "apartment_buildings")
public class ApartmentBuilding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String address;

    // @ManyToOne
    // @JoinColumn(name = "manager_id")
    // private User manager; // Assuming User entity is in com.example.apartmentmanagerapi.model

    @OneToMany(mappedBy = "apartmentBuilding", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Flat> flats = new ArrayList<>();
    
    /**
     * List of user assignments for this building
     * Multiple managers can be assigned to this building
     */
    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    private List<UserBuildingAssignment> userAssignments = new ArrayList<>();
    
    /**
     * List of expenses for this building
     * One building can have many expenses
     */
    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Expense> expenses = new ArrayList<>();
    
    /**
     * Default monthly maintenance fee for flats in this building
     * Used for automatic monthly due generation
     * Can be null if building doesn't have automatic generation enabled
     */
    @Column(name = "default_monthly_fee", precision = 10, scale = 2)
    private BigDecimal defaultMonthlyFee;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Constructors
    public ApartmentBuilding() {
    }

    public ApartmentBuilding(String name, String address) {
        this.name = name;
        this.address = address;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Flat> getFlats() {
        return flats;
    }

    public void setFlats(List<Flat> flats) {
        this.flats = flats;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods for managing flats
    public void addFlat(Flat flat) {
        flats.add(flat);
        flat.setApartmentBuilding(this);
    }

    public void removeFlat(Flat flat) {
        flats.remove(flat);
        flat.setApartmentBuilding(null);
    }
    
    public List<UserBuildingAssignment> getUserAssignments() {
        return userAssignments;
    }
    
    public void setUserAssignments(List<UserBuildingAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }
    
    public List<Expense> getExpenses() {
        return expenses;
    }
    
    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }
    
    public BigDecimal getDefaultMonthlyFee() {
        return defaultMonthlyFee;
    }
    
    public void setDefaultMonthlyFee(BigDecimal defaultMonthlyFee) {
        this.defaultMonthlyFee = defaultMonthlyFee;
    }
    
    /**
     * Helper method to get active user assignments
     * @return List of currently active assignments only
     */
    public List<UserBuildingAssignment> getActiveUserAssignments() {
        return userAssignments.stream()
                .filter(UserBuildingAssignment::isCurrentlyActive)
                .toList();
    }
    
    /**
     * Helper method to check if a specific user manages this building
     * @param userId The user ID to check
     * @return true if the user has an active assignment to this building
     */
    public boolean isManagedByUser(Long userId) {
        return userAssignments.stream()
                .filter(UserBuildingAssignment::isCurrentlyActive)
                .anyMatch(assignment -> assignment.getUser().getId().equals(userId));
    }
}