package example.timeflows.controller;

import example.timeflows.controller.dto.DivisionRequest;
import example.timeflows.controller.dto.DivisionResponse;
import example.timeflows.controller.dto.DivisionSummaryResponse;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.service.DivisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@RestController
@RequestMapping("/api/divisions")
@Tag(name = "Divisions", description = "CRUD operations for divisions")
public class DivisionController {

    private final DivisionService divisionService;
    private final TimeflowsMapper mapper;

    public DivisionController(DivisionService divisionService, TimeflowsMapper mapper) {
        this.divisionService = divisionService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Get all divisions")
    public List<DivisionSummaryResponse> findAll() {
        return mapper.toDivisionSummaries(divisionService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Get division by id")
    public DivisionResponse findById(@PathVariable Long id) {
        return mapper.toDivisionResponse(divisionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create division")
    public DivisionResponse create(@Valid @RequestBody DivisionRequest request) {
        return mapper.toDivisionResponse(
                divisionService.create(mapper.toDivision(request), request.getDepartmentId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @Operation(summary = "Update division")
    public DivisionResponse update(
            @PathVariable Long id, @Valid @RequestBody DivisionRequest request) {
        return mapper.toDivisionResponse(
                divisionService.update(id, mapper.toDivision(request), request.getDepartmentId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete division")
    public void delete(@PathVariable Long id) {
        divisionService.delete(id);
    }
}
