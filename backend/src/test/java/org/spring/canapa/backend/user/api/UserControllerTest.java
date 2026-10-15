package org.spring.canapa.backend.user.api;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.api.error.GlobalExceptionHandler;
import org.spring.canapa.backend.user.UserService;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UpdateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.spring.canapa.backend.user.exception.DuplicateUserEmailException;
import org.spring.canapa.backend.user.exception.InvalidUserDataException;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-07T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.createUser(new CreateUserCommand("Alice", "alice@example.com")))
                .thenReturn(new UserData(userId, "Alice", "alice@example.com", CREATED_AT));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice",
                                  "email": "alice@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + userId))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-07T12:00:00Z"));
    }

    @Test
    void retrievesUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUser(userId))
                .thenReturn(new UserData(userId, "Alice", "alice@example.com", CREATED_AT));

        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void updatesUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.updateUser(
                userId,
                new UpdateUserCommand("Alice Smith", "alice.smith@example.com")
        )).thenReturn(new UserData(
                userId,
                "Alice Smith",
                "alice.smith@example.com",
                CREATED_AT
        ));

        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Alice Smith",
                                  "email": "alice.smith@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));
    }

    @Test
    void mapsInvalidUserDataToBadRequest() throws Exception {
        when(userService.createUser(new CreateUserCommand("", "invalid")))
                .thenThrow(new InvalidUserDataException("name", "must not be blank"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "email": "invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid name: must not be blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/users"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void mapsMissingUserToNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUser(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/users/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/users/" + userId));
    }

    @Test
    void mapsDuplicateEmailToConflict() throws Exception {
        when(userService.createUser(new CreateUserCommand("Alice", "alice@example.com")))
                .thenThrow(new DuplicateUserEmailException("alice@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Alice", "email": "alice@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USER_DATA"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void mapsMalformedUuidToBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for userId"))
                .andExpect(jsonPath("$.path").value("/api/users/not-a-uuid"));
    }

    @Test
    void mapsMalformedJsonToBadRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }
}
