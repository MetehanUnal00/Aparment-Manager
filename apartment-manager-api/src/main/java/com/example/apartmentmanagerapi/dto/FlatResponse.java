package com.example.apartmentmanagerapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FlatResponse {
    private Long id;
    private String flatNumber;
    private Integer numberOfRooms;
    private BigDecimal areaSqMeters;
    private Long apartmentBuildingId;
    private String apartmentBuildingName; // For convenience
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Tenant information fields
    private String tenantName;
    private String tenantContact;
    private String tenantEmail;
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private LocalDateTime tenantMoveInDate;
    private Boolean isActive;
    private BigDecimal currentBalance;

    // Constructor
    public FlatResponse(Long id, String flatNumber, Integer numberOfRooms, BigDecimal areaSqMeters,
                        Long apartmentBuildingId, String apartmentBuildingName,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.flatNumber = flatNumber;
        this.numberOfRooms = numberOfRooms;
        this.areaSqMeters = areaSqMeters;
        this.apartmentBuildingId = apartmentBuildingId;
        this.apartmentBuildingName = apartmentBuildingName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public Integer getNumberOfRooms() {
        return numberOfRooms;
    }

    public void setNumberOfRooms(Integer numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public BigDecimal getAreaSqMeters() {
        return areaSqMeters;
    }

    public void setAreaSqMeters(BigDecimal areaSqMeters) {
        this.areaSqMeters = areaSqMeters;
    }

    public Long getApartmentBuildingId() {
        return apartmentBuildingId;
    }

    public void setApartmentBuildingId(Long apartmentBuildingId) {
        this.apartmentBuildingId = apartmentBuildingId;
    }

    public String getApartmentBuildingName() {
        return apartmentBuildingName;
    }

    public void setApartmentBuildingName(String apartmentBuildingName) {
        this.apartmentBuildingName = apartmentBuildingName;
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
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
    
    public String getTenantContact() {
        return tenantContact;
    }
    
    public void setTenantContact(String tenantContact) {
        this.tenantContact = tenantContact;
    }
    
    public String getTenantEmail() {
        return tenantEmail;
    }
    
    public void setTenantEmail(String tenantEmail) {
        this.tenantEmail = tenantEmail;
    }
    
    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }
    
    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }
    
    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }
    
    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }
    
    public LocalDateTime getTenantMoveInDate() {
        return tenantMoveInDate;
    }
    
    public void setTenantMoveInDate(LocalDateTime tenantMoveInDate) {
        this.tenantMoveInDate = tenantMoveInDate;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }
}