package example.timeflows.controller;

import example.timeflows.controller.dto.DepartmentRequest;
import example.timeflows.controller.dto.DepartmentResponse;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.service.DepartmentService;
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
@RequestMapping("/api/departments")
@Tag(name = "Departments", description = "CRUD operations for departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final TimeflowsMapper mapper;

    public DepartmentController(DepartmentService departmentService, TimeflowsMapper mapper) {
        this.departmentService = departmentService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Get all departments")
    public List<DepartmentResponse> findAll() {
        return mapper.toDepartmentResponses(departmentService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Get department by id")
    public DepartmentResponse findById(@PathVariable Long id) {
        return mapper.toDepartmentResponse(departmentService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create department")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return mapper.toDepartmentResponse(departmentService.create(mapper.toDepartment(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @Operation(summary = "Update department")
    public DepartmentResponse update(
            @PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return mapper.toDepartmentResponse(
                departmentService.update(id, mapper.toDepartment(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ABSOLUT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete department")
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }
}
