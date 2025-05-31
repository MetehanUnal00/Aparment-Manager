package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.mapper.ApartmentBuildingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApartmentBuildingService implements IApartmentBuildingService {

    private final ApartmentBuildingRepository apartmentBuildingRepository;
    private final ApartmentBuildingMapper apartmentBuildingMapper;

    @Transactional
    public ApartmentBuildingResponse createApartmentBuilding(ApartmentBuildingRequest request) {
        if (apartmentBuildingRepository.findByName(request.getName()).isPresent()) {
            // Consider a custom exception here
            throw new RuntimeException("Error: Apartment building name is already taken!");
        }

        ApartmentBuilding building = apartmentBuildingMapper.toEntity(request);
        ApartmentBuilding savedBuilding = apartmentBuildingRepository.save(building);

        return apartmentBuildingMapper.toResponse(savedBuilding);
    }

    @Transactional(readOnly = true)
    public List<ApartmentBuildingResponse> getAllApartmentBuildings() {
        return apartmentBuildingRepository.findAll().stream()
                .map(apartmentBuildingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ApartmentBuildingResponse getApartmentBuildingById(Long id) {
        ApartmentBuilding building = apartmentBuildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + id)); // Consider ResourceNotFoundException
        return apartmentBuildingMapper.toResponse(building);
    }

    @Transactional
    public ApartmentBuildingResponse updateApartmentBuilding(Long id, ApartmentBuildingRequest request) {
        ApartmentBuilding building = apartmentBuildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + id));

        // Check if name is being changed and if the new name is already taken by another building
        if (!building.getName().equals(request.getName()) && apartmentBuildingRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Error: New apartment building name is already taken!");
        }

        // Update the entity using mapper
        apartmentBuildingMapper.updateEntityFromRequest(request, building);
        ApartmentBuilding updatedBuilding = apartmentBuildingRepository.save(building);
        return apartmentBuildingMapper.toResponse(updatedBuilding);
    }

    @Transactional
    public void deleteApartmentBuilding(Long id) {
        if (!apartmentBuildingRepository.existsById(id)) {
            throw new RuntimeException("Error: Apartment building not found with id: " + id);
        }
        // Consider implications: what happens to flats in this building?
        // For now, simple delete. Later, might need to check if flats exist.
        apartmentBuildingRepository.deleteById(id);
    }
}