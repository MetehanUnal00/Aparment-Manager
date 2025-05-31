package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.ApartmentBuildingRequest;
import com.example.apartmentmanagerapi.dto.ApartmentBuildingResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.service.IApartmentBuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Apartment Buildings", description = "Manage apartment buildings")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/apartment-buildings")
public class ApartmentBuildingController {

    @Autowired
    private IApartmentBuildingService apartmentBuildingService;

    @Operation(
        summary = "Create apartment building",
        description = "Creates a new apartment building. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Building created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApartmentBuildingResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or building name already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER or ADMIN role"
        )
    })
    @PostMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> createApartmentBuilding(@Valid @RequestBody ApartmentBuildingRequest request) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.createApartmentBuilding(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Get all apartment buildings",
        description = "Retrieves all apartment buildings. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of buildings retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = ApartmentBuildingResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER or ADMIN role"
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ApartmentBuildingResponse>> getAllApartmentBuildings() {
        List<ApartmentBuildingResponse> buildings = apartmentBuildingService.getAllApartmentBuildings();
        return ResponseEntity.ok(buildings);
    }

    @Operation(
        summary = "Get apartment building by ID",
        description = "Retrieves a specific apartment building by its ID. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Building retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApartmentBuildingResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Building not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER or ADMIN role"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getApartmentBuildingById(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long id) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.getApartmentBuildingById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Update apartment building",
        description = "Updates an existing apartment building. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Building updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApartmentBuildingResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors, building not found, or new name already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER or ADMIN role"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateApartmentBuilding(
            @Parameter(description = "ID of the apartment building to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ApartmentBuildingRequest request) {
        try {
            ApartmentBuildingResponse response = apartmentBuildingService.updateApartmentBuilding(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Delete apartment building",
        description = "Deletes an apartment building. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Building deleted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Building not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - JWT token is missing or invalid"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - User does not have MANAGER or ADMIN role"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteApartmentBuilding(
            @Parameter(description = "ID of the apartment building to delete", required = true)
            @PathVariable Long id) {
        try {
            apartmentBuildingService.deleteApartmentBuilding(id);
            return ResponseEntity.ok(new MessageResponse("Apartment building deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}