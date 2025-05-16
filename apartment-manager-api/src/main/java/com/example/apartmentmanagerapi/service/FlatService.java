package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlatService {

    @Autowired
    private FlatRepository flatRepository;

    @Autowired
    private ApartmentBuildingRepository apartmentBuildingRepository;

    @Transactional
    public FlatResponse createFlat(FlatRequest request) {
        ApartmentBuilding building = apartmentBuildingRepository.findById(request.getApartmentBuildingId())
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + request.getApartmentBuildingId()));

        if (flatRepository.findByApartmentBuildingIdAndFlatNumber(request.getApartmentBuildingId(), request.getFlatNumber()).isPresent()) {
            throw new RuntimeException("Error: Flat number " + request.getFlatNumber() + " already exists in this building.");
        }

        Flat flat = new Flat(
                request.getFlatNumber(),
                request.getNumberOfRooms(),
                request.getAreaSqMeters(),
                building
        );
        Flat savedFlat = flatRepository.save(flat);
        return mapToResponse(savedFlat);
    }

    @Transactional(readOnly = true)
    public List<FlatResponse> getAllFlatsByBuildingId(Long buildingId) {
        if (!apartmentBuildingRepository.existsById(buildingId)) {
            throw new RuntimeException("Error: Apartment building not found with id: " + buildingId);
        }
        return flatRepository.findByApartmentBuildingId(buildingId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FlatResponse getFlatById(Long buildingId, Long flatId) {
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        return mapToResponse(flat);
    }

    @Transactional
    public FlatResponse updateFlat(Long buildingId, Long flatId, FlatRequest request) {
        ApartmentBuilding building = apartmentBuildingRepository.findById(buildingId)
                .orElseThrow(() -> new RuntimeException("Error: Apartment building not found with id: " + buildingId));

        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));

        // Check if flat number is being changed and if the new number is already taken in the same building
        if (!flat.getFlatNumber().equals(request.getFlatNumber()) &&
            flatRepository.findByApartmentBuildingIdAndFlatNumber(buildingId, request.getFlatNumber()).isPresent()) {
            throw new RuntimeException("Error: New flat number " + request.getFlatNumber() + " already exists in this building.");
        }

        flat.setFlatNumber(request.getFlatNumber());
        flat.setNumberOfRooms(request.getNumberOfRooms());
        flat.setAreaSqMeters(request.getAreaSqMeters());
        // Note: Changing apartmentBuildingId for an existing flat might be complex or disallowed.
        // For now, we assume the flat stays within the same building or this request.getApartmentBuildingId() matches the current buildingId.
        // If you need to move a flat to a different building, that's a more complex operation.
        if (!buildingId.equals(request.getApartmentBuildingId())) {
             ApartmentBuilding newBuilding = apartmentBuildingRepository.findById(request.getApartmentBuildingId())
                .orElseThrow(() -> new RuntimeException("Error: New Apartment building not found with id: " + request.getApartmentBuildingId()));
            flat.setApartmentBuilding(newBuilding);
        }


        Flat updatedFlat = flatRepository.save(flat);
        return mapToResponse(updatedFlat);
    }

    @Transactional
    public void deleteFlat(Long buildingId, Long flatId) {
        if (!flatRepository.existsById(flatId)) {
             throw new RuntimeException("Error: Flat not found with id: " + flatId);
        }
        Flat flat = flatRepository.findByApartmentBuildingIdAndId(buildingId, flatId)
                .orElseThrow(() -> new RuntimeException("Error: Flat not found with id: " + flatId + " in building: " + buildingId));
        // Consider implications: what happens to tenants in this flat?
        flatRepository.delete(flat);
    }


    private FlatResponse mapToResponse(Flat flat) {
        return new FlatResponse(
                flat.getId(),
                flat.getFlatNumber(),
                flat.getNumberOfRooms(),
                flat.getAreaSqMeters(),
                flat.getApartmentBuilding().getId(),
                flat.getApartmentBuilding().getName(),
                flat.getCreatedAt(),
                flat.getUpdatedAt()
        );
    }
}