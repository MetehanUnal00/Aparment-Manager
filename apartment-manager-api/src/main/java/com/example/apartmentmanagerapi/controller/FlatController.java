package com.example.apartmentmanagerapi.controller;

import com.example.apartmentmanagerapi.dto.FlatRequest;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.dto.MessageResponse;
import com.example.apartmentmanagerapi.service.IFlatService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Flats", description = "Manage flats within apartment buildings")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/apartment-buildings/{buildingId}/flats")
@RequiredArgsConstructor
public class FlatController {

    private final IFlatService flatService;

    @Operation(
        summary = "Create a new flat",
        description = "Creates a new flat in the specified apartment building. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flat created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FlatResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or flat number already exists",
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
    public ResponseEntity<?> createFlat(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Valid @RequestBody FlatRequest request) {
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

    @Operation(
        summary = "Get all flats in a building",
        description = "Retrieves all flats in the specified apartment building. Requires MANAGER, ADMIN, or TENANT role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of flats retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FlatResponse.class))
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
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('TENANT')") // Tenants might view flats in their building
    public ResponseEntity<?> getAllFlatsByBuilding(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId) {
         try {
            List<FlatResponse> flats = flatService.getAllFlatsByBuildingId(buildingId);
            return ResponseEntity.ok(flats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Get flat by ID",
        description = "Retrieves a specific flat by its ID. Requires MANAGER, ADMIN, or TENANT role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flat retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FlatResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Flat not found",
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
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN') or hasRole('TENANT')") // Tenant might view their specific flat
    public ResponseEntity<?> getFlatById(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId) {
        try {
            FlatResponse response = flatService.getFlatById(buildingId, flatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(
        summary = "Update flat",
        description = "Updates an existing flat's information. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flat updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FlatResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors, flat not found, or building ID mismatch",
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
    @PutMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateFlat(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat to update", required = true)
            @PathVariable Long flatId,
            @Valid @RequestBody FlatRequest request) {
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

    @Operation(
        summary = "Delete flat",
        description = "Deletes a flat from the building. Requires MANAGER or ADMIN role. Note: Consider using deactivate instead for audit trail."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flat deleted successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MessageResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Flat not found",
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
    @DeleteMapping("/{flatId}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteFlat(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat to delete", required = true)
            @PathVariable Long flatId) {
        try {
            flatService.deleteFlat(buildingId, flatId);
            return ResponseEntity.ok(new MessageResponse("Flat deleted successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get active flats",
        description = "Retrieves only active flats in the building (isActive=true). Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active flats retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = FlatResponse.class))
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
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getActiveFlatsByBuilding(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId) {
        try {
            List<FlatResponse> flats = flatService.getActiveFlatsByBuildingId(buildingId);
            return ResponseEntity.ok(flats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Get flat with financial information",
        description = "Retrieves comprehensive financial information for a flat including balance, payments, and dues. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Financial information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Flat not found",
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
            description = "Forbidden - User does not have required role"
        )
    })
    @GetMapping("/{flatId}/financial-info")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> getFlatWithFinancialInfo(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId) {
        try {
            Map<String, Object> flatInfo = flatService.getFlatWithFinancialInfo(buildingId, flatId);
            return ResponseEntity.ok(flatInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Update tenant information",
        description = "Updates only the tenant-related information for a flat. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant information updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FlatResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - validation errors or flat not found",
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
    @PutMapping("/{flatId}/tenant")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateTenantInfo(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat", required = true)
            @PathVariable Long flatId,
            @Valid @RequestBody FlatRequest request) {
        try {
            FlatResponse response = flatService.updateTenantInfo(buildingId, flatId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @Operation(
        summary = "Deactivate a flat",
        description = "Marks a flat as inactive (soft delete). Inactive flats won't be included in monthly due generation. Requires MANAGER or ADMIN role."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Flat deactivated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FlatResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Flat not found",
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
            description = "Forbidden - User does not have required role"
        )
    })
    @PutMapping("/{flatId}/deactivate")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<?> deactivateFlat(
            @Parameter(description = "ID of the apartment building", required = true)
            @PathVariable Long buildingId,
            @Parameter(description = "ID of the flat to deactivate", required = true)
            @PathVariable Long flatId) {
        try {
            FlatResponse response = flatService.deactivateFlat(buildingId, flatId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}