package com.iodsky.mysweldo.position;

import com.iodsky.mysweldo.common.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/positions")
@PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Positions", description = "Position management endpoints")
public class PositionController {

    private final PositionService service;
    private final PositionMapper mapper;

    @PostMapping
    @Operation(
            summary = "Create a new position",
            description = "Create a new position. Requires HR role.",
            operationId = "createPosition"
    )
    public ResponseEntity<ApiResponse<PositionDto>> createPosition(@Valid @RequestBody PositionRequest request) {
        PositionDto position = mapper.toDto(service.createPosition(request));
        return ResponseFactory.created("Position created successfully", position);
    }

    @GetMapping
    @Operation(summary = "Get all positions", description = "Retrieve a paginated list of positions. Requires HR role.")
    public ResponseEntity<ApiResponse<List<PositionDto>>> getAllPositions(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<Position> page = service.getAllPositions(pageNo, limit);

        List<PositionDto> positions = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "Positions retrieved successfully",
                positions,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get position by ID", description = "Retrieve a specific position by its ID. Requires HR role.")
    public ResponseEntity<ApiResponse<PositionDto>> getPositionById(
            @Parameter(description = "Position ID") @PathVariable String id
    ) {
        PositionDto position = mapper.toDto(service.getPositionById(id));
        return ResponseFactory.ok("Position retrieved successfully", position);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update position", description = "Update an existing position's information. Requires HR role.")
    public ResponseEntity<ApiResponse<PositionDto>> updatePosition(
            @Parameter(description = "Position ID") @PathVariable String id,
            @Valid @RequestBody PositionUpdateRequest request
    ) {
        PositionDto position = mapper.toDto(service.updatePosition(id, request));
        return ResponseFactory.ok("Position updated successfully", position);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete position", description = "Delete a position. Requires HR role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePosition(
            @Parameter(description = "Position ID") @PathVariable String id
    ) {
        service.deletePosition(id);
        DeleteResponse res = new DeleteResponse("Position", id);
        return ResponseFactory.ok("Position deleted successfully", res);
    }
}
