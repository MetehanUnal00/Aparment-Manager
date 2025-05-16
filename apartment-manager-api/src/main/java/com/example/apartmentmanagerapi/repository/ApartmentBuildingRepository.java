package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApartmentBuildingRepository extends JpaRepository<ApartmentBuilding, Long> {
    Optional<ApartmentBuilding> findByName(String name);
}