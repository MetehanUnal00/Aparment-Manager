package com.example.apartmentmanagerapi.dto;

import java.time.LocalDateTime;

public class FlatResponse {
    private Long id;
    private String flatNumber;
    private Integer numberOfRooms;
    private Double areaSqMeters;
    private Long apartmentBuildingId;
    private String apartmentBuildingName; // For convenience
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor
    public FlatResponse(Long id, String flatNumber, Integer numberOfRooms, Double areaSqMeters,
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
}