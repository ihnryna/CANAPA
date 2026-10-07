package org.spring.canapa.backend.user.api;

import org.spring.canapa.backend.user.UserService;
import org.spring.canapa.backend.user.api.dto.CreateUserRequest;
import org.spring.canapa.backend.user.api.dto.UpdateUserRequest;
import org.spring.canapa.backend.user.api.dto.UserResponse;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UpdateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserData created = userService.createUser(
                new CreateUserCommand(request.name(), request.email())
        );
        UserResponse response = toResponse(created);

        return ResponseEntity
                .created(URI.create("/api/users/" + response.id()))
                .body(response);
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable UUID userId) {
        return toResponse(userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public UserResponse updateUser(
            @PathVariable UUID userId,
            @RequestBody UpdateUserRequest request
    ) {
        return toResponse(userService.updateUser(
                userId,
                new UpdateUserCommand(request.name(), request.email())
        ));
    }

    private UserResponse toResponse(UserData user) {
        return new UserResponse(
                user.id(),
                user.name(),
                user.email(),
                user.createdAt()
        );
    }
}
