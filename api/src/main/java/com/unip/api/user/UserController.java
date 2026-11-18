package com.unip.api.user;

import com.unip.api.security.CurrentUser;
import com.unip.api.user.dto.UserRequest;
import com.unip.api.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    public UserController(UserService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(service.create(req)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ENVIRONMENTAL_OFFICER','MANAGER')")
    public List<UserResponse> list() {
        return service.listActive().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(currentUser.get());
    }
}