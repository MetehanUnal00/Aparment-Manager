package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Long> {
    List<Flat> findByApartmentBuildingId(Long buildingId);
    Optional<Flat> findByApartmentBuildingIdAndFlatNumber(Long buildingId, String flatNumber);
    Optional<Flat> findByApartmentBuildingIdAndId(Long buildingId, Long flatId);
    
    /**
     * Find all active flats for a building
     * @param buildingId The building ID
     * @return List of active flats
     */
    List<Flat> findByApartmentBuildingIdAndIsActiveTrue(Long buildingId);
}