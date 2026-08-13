package example.timeflows.controller;

import example.timeflows.controller.dto.OvertimeDecisionRequest;
import example.timeflows.controller.dto.OvertimeRequest;
import example.timeflows.controller.dto.OvertimeResponse;
import example.timeflows.exception.UserException;
import example.timeflows.model.Overtime;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import example.timeflows.service.OvertimeService;
import example.timeflows.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/overtimes")
@Tag(name = "Overtimes", description = "CRUD operations for employee overtime records")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final UserService userService;

    public OvertimeController(OvertimeService overtimeService, UserService userService) {
        this.overtimeService = overtimeService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get current user's overtime records for a selected month")
    public List<OvertimeResponse> findMonth(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication
    ) {
        return overtimeService.findMonth(authentication.getName(), YearMonth.of(year, month)).stream()
                .map(OvertimeResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get current user's overtime by id")
    public OvertimeResponse findById(@PathVariable Long id, Authentication authentication) {
        return OvertimeResponse.from(overtimeService.findByIdForUser(id, authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create current user's overtime")
    public OvertimeResponse create(@Valid @RequestBody OvertimeRequest request, Authentication authentication) {
        return OvertimeResponse.from(overtimeService.create(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update current user's pending overtime")
    public OvertimeResponse update(
            @PathVariable Long id,
            @Valid @RequestBody OvertimeRequest request,
            Authentication authentication
    ) {
        return OvertimeResponse.from(overtimeService.update(authentication.getName(), id, request));
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
            @Validated({Default.class, OvertimeRequest.Resubmission.class}) @RequestBody OvertimeRequest request,
            Authentication authentication
    ) {
        return OvertimeResponse.from(overtimeService.resubmit(authentication.getName(), id, request));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Approve overtime")
    public OvertimeResponse approve(
            @PathVariable Long id,
            @RequestBody OvertimeDecisionRequest request,
            Authentication authentication
    ) {
        assertCanReview(id, authentication);
        return OvertimeResponse.from(overtimeService.approve(id, request.getManagerComment()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Reject overtime")
    public OvertimeResponse reject(
            @PathVariable Long id,
            @RequestBody OvertimeDecisionRequest request,
            Authentication authentication
    ) {
        assertCanReview(id, authentication);
        return OvertimeResponse.from(overtimeService.reject(id, request.getManagerComment()));
    }

    private void assertCanReview(Long overtimeId, Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        if (currentUser.getRoles().contains(Role.ADMIN)) {
            return;
        }
        Overtime overtime = overtimeService.findById(overtimeId);
        if (!overtime.getUser().getDivision().getId().equals(currentUser.getDivision().getId())) {
            throw new UserException("Керівник може переглядати тільки overtime свого відділу");
        }
    }
}
