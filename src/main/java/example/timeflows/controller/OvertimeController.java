package example.timeflows.controller;

import example.timeflows.controller.dto.OvertimeDecisionRequest;
import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.controller.dto.OvertimeResponse;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.service.OvertimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/overtimes")
@Tag(name = "Overtimes", description = "CRUD operations for employee overtime records")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final TimeflowsMapper mapper;

    public OvertimeController(OvertimeService overtimeService, TimeflowsMapper mapper) {
        this.overtimeService = overtimeService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get current user's overtime records for a selected month")
    public List<OvertimeResponse> findMonth(
            @RequestParam int year, @RequestParam int month, Authentication authentication) {
        return mapper.toOvertimeResponses(
                overtimeService.findMonth(authentication.getName(), YearMonth.of(year, month)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current user's overtime by id")
    public OvertimeResponse findById(@PathVariable Long id, Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.findByIdForUser(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create current user's overtime")
    public OvertimeResponse create(
            @Valid @RequestBody OvertimeRequest request, Authentication authentication) {
        return mapper.toOvertimeResponse(overtimeService.create(authentication.getName(), request));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER','ABSOLUT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create overtime for an employee in the manager's division")
    public OvertimeResponse createForDivisionEmployee(
            @PathVariable Long userId,
            @Valid @RequestBody OvertimeRequest request,
            Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.createForDivisionEmployee(
                        authentication.getName(), userId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update current user's pending overtime")
    public OvertimeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OvertimeRequest request,
            Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.update(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete current user's pending overtime")
    public void delete(@PathVariable Long id, Authentication authentication) {
        overtimeService.delete(authentication.getName(), id);
    }

    @PostMapping("/{id}/resubmit")
    @Operation(summary = "Send rejected overtime for approval again")
    public OvertimeResponse resubmit(
            @PathVariable Long id,
            @Validated({Default.class, OvertimeRequest.Resubmission.class}) @RequestBody
                    OvertimeRequest request,
            Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.resubmit(authentication.getName(), id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Approve overtime")
    public OvertimeResponse approve(
            @PathVariable Long id,
            @RequestBody OvertimeDecisionRequest request,
            Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.approve(id, request.getManagerComment(), authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','ABSOLUT')")
    @Operation(summary = "Reject overtime")
    public OvertimeResponse reject(
            @PathVariable Long id,
            @RequestBody OvertimeDecisionRequest request,
            Authentication authentication) {
        return mapper.toOvertimeResponse(
                overtimeService.reject(id, request.getManagerComment(), authentication.getName()));
    }
}
