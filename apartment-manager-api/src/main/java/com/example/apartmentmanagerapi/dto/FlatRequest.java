package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FlatRequest {

    @NotBlank
    @Size(min = 1, max = 10)
    private String flatNumber;

    @Positive
    private Integer numberOfRooms;

    @Positive
    private Double areaSqMeters;

    @NotNull
    private Long apartmentBuildingId; // To link to an existing ApartmentBuilding
    
    // Tenant information fields
    @Size(max = 100)
    private String tenantName;
    
    @Size(max = 50)
    private String tenantContact;
    
    @Email
    @Size(max = 100)
    private String tenantEmail;
    
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal monthlyRent;
    
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal securityDeposit;
    
    private LocalDateTime tenantMoveInDate;
    
    private Boolean isActive;

    // Getters and Setters
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

    public Double getAreaSqMeters() {
        return areaSqMeters;
    }

    public void setAreaSqMeters(Double areaSqMeters) {
        this.areaSqMeters = areaSqMeters;
    }

    public Long getApartmentBuildingId() {
        return apartmentBuildingId;
    }

    public void setApartmentBuildingId(Long apartmentBuildingId) {
        this.apartmentBuildingId = apartmentBuildingId;
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
}