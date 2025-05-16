package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    // We can add a Tenant relationship later
    // @OneToOne(mappedBy = "flat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // private Tenant tenant;

    @NotNull // A flat must belong to an apartment building
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_building_id", nullable = false)
    private ApartmentBuilding apartmentBuilding;

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