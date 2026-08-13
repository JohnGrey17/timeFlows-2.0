package example.timeflows.controller;

import example.timeflows.controller.dto.DivisionRequest;
import example.timeflows.controller.dto.DivisionResponse;
import example.timeflows.controller.dto.DivisionSummaryResponse;
import example.timeflows.model.Division;
import example.timeflows.service.DivisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/divisions")
@Tag(name = "Divisions", description = "CRUD operations for divisions")
public class DivisionController {

    private final DivisionService divisionService;

    public DivisionController(DivisionService divisionService) {
        this.divisionService = divisionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get all divisions")
    public List<DivisionSummaryResponse> findAll() {
        return divisionService.findAll().stream()
                .map(DivisionSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get division by id")
    public DivisionResponse findById(@PathVariable Long id) {
        return DivisionResponse.from(divisionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create division")
    public DivisionResponse create(@Valid @RequestBody DivisionRequest request) {
        return DivisionResponse.from(divisionService.create(toDivision(request), request.getDepartmentId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update division")
    public DivisionResponse update(@PathVariable Long id, @Valid @RequestBody DivisionRequest request) {
        return DivisionResponse.from(divisionService.update(id, toDivision(request), request.getDepartmentId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete division")
    public void delete(@PathVariable Long id) {
        divisionService.delete(id);
    }

    private Division toDivision(DivisionRequest request) {
        Division division = new Division();
        division.setName(request.getName());
        return division;
    }
}
