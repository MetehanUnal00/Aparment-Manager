package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.service.ApartmentBuildingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/apartment-buildings")
public class ApartmentBuildingController {

    @Autowired
    private ApartmentBuildingService apartmentBuildingService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createApartmentBuilding(@Valid @RequestBody ApartmentBuildingRequest request) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.createApartmentBuilding(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<ApartmentBuildingResponse>> getAllApartmentBuildings() {
        List<ApartmentBuildingResponse> buildings = apartmentBuildingService.getAllApartmentBuildings();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getApartmentBuildingById(@PathVariable Long id) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.getApartmentBuildingById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateApartmentBuilding(@PathVariable Long id, @Valid @RequestBody ApartmentBuildingRequest request) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.updateApartmentBuilding(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteApartmentBuilding(@PathVariable Long id) {
        try {
            apartmentBuildingService.deleteApartmentBuilding(id);
            return ResponseEntity.ok(new MessageResponse("Apartment building deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}