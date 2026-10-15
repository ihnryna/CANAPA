package org.spring.canapa.backend.room.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.user.UserService;
import org.spring.canapa.backend.user.dto.CreateUserCommand;
import org.spring.canapa.backend.user.dto.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoomApiIntegrationTest {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void createsRetrievesJoinsAndAdvancesRoomThroughHttp() throws Exception {
        UserData creator = userService.createUser(
                new CreateUserCommand("Alice", "alice.room.api@example.com")
        );
        UserData participant = userService.createUser(
                new CreateUserCommand("Bob", "bob.room.api@example.com")
        );

        MvcResult createResult = mockMvc.perform(post("/api/rooms")
                        .header(USER_ID_HEADER, creator.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  Friday movies  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Friday movies"))
                .andExpect(jsonPath("$.creator.id").value(creator.id().toString()))
                .andExpect(jsonPath("$.participants[0].id").value(creator.id().toString()))
                .andExpect(jsonPath("$.participants[0].email").doesNotExist())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String roomId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId));

        mockMvc.perform(post("/api/rooms/{roomId}/participants", roomId)
                        .header(USER_ID_HEADER, participant.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[1].id").value(participant.id().toString()));

        mockMvc.perform(post("/api/rooms/{roomId}/participants", roomId)
                        .header(USER_ID_HEADER, participant.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_PARTICIPANT_ALREADY_EXISTS"));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, creator.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "VOTING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOTING"));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, participant.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FINISHED"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACTION_NOT_ALLOWED"));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, creator.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FINISHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.participants.length()").value(2));
    }
}
