package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Flat entity represents an individual apartment unit within an apartment building.
 * Each flat can have tenant information, associated payments, and monthly dues.
 * 
 * The current balance is calculated dynamically based on dues and payments.
 * Tenant information is stored as simple fields for MVP, with a clear migration path
 * to a separate Tenant entity in the future if needed.
 */
@Entity
@Table(name = "flats")
public class Flat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String flatNumber; // e.g., "1A", "101"

    private Integer numberOfRooms;

    private Double areaSqMeters; // Area in square meters
    
    /**
     * Tenant information - MVP approach using simple fields
     * In the future, this can be migrated to a separate Tenant entity
     * for more complex scenarios (multiple tenants, lease tracking, etc.)
     */
    
    /**
     * Name of the current tenant
     */
    @Column(name = "tenant_name", length = 100)
    private String tenantName;
    
    /**
     * Contact number of the current tenant
     */
    @Column(name = "tenant_contact", length = 50)
    private String tenantContact;
    
    /**
     * Email address of the current tenant
     */
    @Email
    @Column(name = "tenant_email", length = 100)
    private String tenantEmail;
    
    /**
     * Monthly rent amount for this flat
     * Used as the base amount when generating monthly dues
     */
    @Column(name = "monthly_rent", precision = 10, scale = 2)
    private BigDecimal monthlyRent;
    
    /**
     * Security deposit amount held for this flat
     */
    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;
    
    /**
     * Date when the current tenant moved in
     * Helps track tenancy duration
     */
    @Column(name = "tenant_move_in_date")
    private LocalDateTime tenantMoveInDate;
    
    /**
     * Whether this flat is currently active and available for dues
     * Inactive flats won't be included in monthly due generation
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @NotNull // A flat must belong to an apartment building
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_building_id", nullable = false)
    private ApartmentBuilding apartmentBuilding;
    
    /**
     * List of all payments made for this flat
     * One-to-many relationship: one flat can have many payments
     */
    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();
    
    /**
     * List of all monthly dues for this flat
     * One-to-many relationship: one flat can have many monthly dues
     */
    @OneToMany(mappedBy = "flat", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MonthlyDue> monthlyDues = new ArrayList<>();
    
    /**
     * Current balance calculated as total unpaid dues minus total payments
     * This is a calculated field using @Formula for small-medium datasets
     * For larger datasets, consider using a database view or stored procedure
     * 
     * Note: This formula calculates the balance as:
     * (Sum of all dues) - (Sum of all payments)
     * A positive value means the tenant owes money
     * A negative value means the tenant has overpaid/has credit
     */
    @Formula("(SELECT COALESCE(SUM(md.due_amount), 0) - COALESCE(SUM(p.amount), 0) " +
             "FROM monthly_dues md " +
             "LEFT JOIN payments p ON p.flat_id = md.flat_id " +
             "WHERE md.flat_id = id)")
    private BigDecimal currentBalance;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Constructors
    public Flat() {
    }

    public Flat(String flatNumber, Integer numberOfRooms, Double areaSqMeters, ApartmentBuilding apartmentBuilding) {
        this.flatNumber = flatNumber;
        this.numberOfRooms = numberOfRooms;
        this.areaSqMeters = areaSqMeters;
        this.apartmentBuilding = apartmentBuilding;
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

    public Double getAreaSqMeters() {
        return areaSqMeters;
    }

    public void setAreaSqMeters(Double areaSqMeters) {
        this.areaSqMeters = areaSqMeters;
    }

    public ApartmentBuilding getApartmentBuilding() {
        return apartmentBuilding;
    }

    public void setApartmentBuilding(ApartmentBuilding apartmentBuilding) {
        this.apartmentBuilding = apartmentBuilding;
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

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<MonthlyDue> getMonthlyDues() {
        return monthlyDues;
    }

    public void setMonthlyDues(List<MonthlyDue> monthlyDues) {
        this.monthlyDues = monthlyDues;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Helper method to check if flat has an active tenant
     * @return true if tenant name is not null or empty
     */
    public boolean hasActiveTenant() {
        return tenantName != null && !tenantName.trim().isEmpty();
    }

    // equals and hashCode (optional, but good practice if adding to collections)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flat flat = (Flat) o;
        return id != null && id.equals(flat.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}