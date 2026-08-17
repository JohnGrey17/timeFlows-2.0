package example.timeflows.controller;

import example.timeflows.controller.dto.DeactivateUserRequest;
import example.timeflows.controller.dto.UserRequest;
import example.timeflows.controller.dto.UserResponse;
import example.timeflows.mapper.TimeflowsMapper;
import example.timeflows.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/users/rest")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users", description = "CRUD operations for application users")
public class UserController {

    private final UserService userService;
    private final TimeflowsMapper mapper;

    public UserController(UserService userService, TimeflowsMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Get all users")
    public List<UserResponse> findAll() {
        return mapper.toUserResponses(userService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id", responses = @ApiResponse(responseCode = "200"))
    public UserResponse findById(@PathVariable Long id) {
        return mapper.toUserResponse(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return mapper.toUserResponse(
                userService.create(mapper.toUser(request), request.getDivisionId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return mapper.toUserResponse(
                userService.update(id, mapper.toUser(request), request.getDivisionId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate user without deleting historical records")
    public void delete(@PathVariable Long id, @RequestBody DeactivateUserRequest request) {
        userService.deactivate(id, request.getReason());
    }
}
