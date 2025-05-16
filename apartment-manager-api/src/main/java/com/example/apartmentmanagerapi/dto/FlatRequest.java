package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
}