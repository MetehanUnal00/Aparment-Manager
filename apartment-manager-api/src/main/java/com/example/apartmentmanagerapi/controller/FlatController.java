package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.service.IFlatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/apartment-buildings/{buildingId}/flats")
@RequiredArgsConstructor
public class FlatController {

    private final IFlatService flatService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> createFlat(@PathVariable Long buildingId, @Valid @RequestBody FlatRequest request) {
        // Ensure the request's buildingId matches the path variable for consistency,
        // or rely on the service to use the path variable primarily.
        if (!buildingId.equals(request.getApartmentBuildingId())) {
             // Or handle this discrepancy as an error, or simply ignore request.getApartmentBuildingId()
             // and always use the path variable. For simplicity, let's ensure they match or update the request.
            request.setApartmentBuildingId(buildingId);
        }
        try {
            FlatResponse response = flatService.createFlat(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('TENANT')") // Tenants might view flats in their building
    public ResponseEntity<?> getAllFlatsByBuilding(@PathVariable Long buildingId) {
         try {
            List<FlatResponse> flats = flatService.getAllFlatsByBuildingId(buildingId);
            return ResponseEntity.ok(flats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('TENANT')") // Tenant might view their specific flat
    public ResponseEntity<?> getFlatById(@PathVariable Long buildingId, @PathVariable Long flatId) {
        try {
            FlatResponse response = flatService.getFlatById(buildingId, flatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateFlat(@PathVariable Long buildingId, @PathVariable Long flatId, @Valid @RequestBody FlatRequest request) {
         if (!buildingId.equals(request.getApartmentBuildingId())) {
            // As in POST, ensure consistency or decide which ID takes precedence.
            // Forcing the request to align with the path for updates makes sense.
            if (request.getApartmentBuildingId() != null && !buildingId.equals(request.getApartmentBuildingId())) {
                 return ResponseEntity.badRequest().body(new MessageResponse("Error: Path buildingId and request buildingId mismatch. Flat cannot be moved this way."));
            }
            request.setApartmentBuildingId(buildingId); // Ensure the service uses the path buildingId
        }
        try {
            FlatResponse response = flatService.updateFlat(buildingId, flatId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteFlat(@PathVariable Long buildingId, @PathVariable Long flatId) {
        try {
            flatService.deleteFlat(buildingId, flatId);
            return ResponseEntity.ok(new MessageResponse("Flat deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Get active flats only
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getActiveFlatsByBuilding(@PathVariable Long buildingId) {
        try {
            List<FlatResponse> flats = flatService.getActiveFlatsByBuildingId(buildingId);
            return ResponseEntity.ok(flats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Get flat with financial information
     */
    @GetMapping("/{flatId}/financial-info")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getFlatWithFinancialInfo(@PathVariable Long buildingId, @PathVariable Long flatId) {
        try {
            Map<String, Object> flatInfo = flatService.getFlatWithFinancialInfo(buildingId, flatId);
            return ResponseEntity.ok(flatInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Update tenant information only
     */
    @PutMapping("/{flatId}/tenant")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> updateTenantInfo(@PathVariable Long buildingId, @PathVariable Long flatId, 
                                               @Valid @RequestBody FlatRequest request) {
        try {
            FlatResponse response = flatService.updateTenantInfo(buildingId, flatId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Deactivate a flat
     */
    @PutMapping("/{flatId}/deactivate")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> deactivateFlat(@PathVariable Long buildingId, @PathVariable Long flatId) {
        try {
            FlatResponse response = flatService.deactivateFlat(buildingId, flatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}