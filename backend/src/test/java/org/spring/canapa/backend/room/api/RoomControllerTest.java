package org.spring.canapa.backend.room.api;

import org.junit.jupiter.api.Test;
import org.spring.canapa.backend.api.error.GlobalExceptionHandler;
import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.RoomStatus;
import org.spring.canapa.backend.room.dto.CreateRoomCommand;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.dto.RoomMemberData;
import org.spring.canapa.backend.room.exception.InvalidRoomDataException;
import org.spring.canapa.backend.room.exception.InvalidRoomStatusTransitionException;
import org.spring.canapa.backend.room.exception.RoomActionNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomJoinNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomNotFoundException;
import org.spring.canapa.backend.room.exception.RoomParticipantAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@Import(GlobalExceptionHandler.class)
class RoomControllerTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final Instant CREATED_AT = Instant.parse("2026-10-07T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @Test
    void createsRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        RoomData room = roomData(roomId, creatorId, RoomStatus.CREATED);
        when(roomService.createRoom(new CreateRoomCommand("Friday movies", creatorId)))
                .thenReturn(room);

        mockMvc.perform(post("/api/rooms")
                        .header(USER_ID_HEADER, creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Friday movies"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/rooms/" + roomId))
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.name").value("Friday movies"))
                .andExpect(jsonPath("$.creator.id").value(creatorId.toString()))
                .andExpect(jsonPath("$.creator.name").value("Alice"))
                .andExpect(jsonPath("$.creator.email").doesNotExist())
                .andExpect(jsonPath("$.participants[0].email").doesNotExist())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-07T12:00:00Z"));
    }

    @Test
    void retrievesRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(roomService.getRoom(roomId))
                .thenReturn(roomData(roomId, creatorId, RoomStatus.CREATED));

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.participants.length()").value(1));
    }

    @Test
    void joinsRoom() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        RoomData joined = new RoomData(
                roomId,
                "Friday movies",
                member(creatorId, "Alice", "alice@example.com"),
                List.of(
                        member(creatorId, "Alice", "alice@example.com"),
                        member(participantId, "Bob", "bob@example.com")
                ),
                RoomStatus.CREATED,
                CREATED_AT
        );
        when(roomService.joinRoom(roomId, participantId)).thenReturn(joined);

        mockMvc.perform(post("/api/rooms/{roomId}/participants", roomId)
                        .header(USER_ID_HEADER, participantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[1].id").value(participantId.toString()));
    }

    @Test
    void changesRoomStatus() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(roomService.changeRoomStatus(roomId, creatorId, RoomStatus.VOTING))
                .thenReturn(roomData(roomId, creatorId, RoomStatus.VOTING));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "VOTING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOTING"));
    }

    @Test
    void mapsInvalidRoomDataToBadRequest() throws Exception {
        UUID creatorId = UUID.randomUUID();
        when(roomService.createRoom(new CreateRoomCommand("", creatorId)))
                .thenThrow(new InvalidRoomDataException("name", "must not be blank"));

        mockMvc.perform(post("/api/rooms")
                        .header(USER_ID_HEADER, creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid name: must not be blank"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/rooms"));
    }

    @Test
    void mapsMissingRoomToNotFound() throws Exception {
        UUID roomId = UUID.randomUUID();
        when(roomService.getRoom(roomId)).thenThrow(new RoomNotFoundException(roomId));

        mockMvc.perform(get("/api/rooms/{roomId}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void mapsDuplicateParticipantToConflict() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(roomService.joinRoom(roomId, userId))
                .thenThrow(new RoomParticipantAlreadyExistsException(roomId, userId));

        mockMvc.perform(post("/api/rooms/{roomId}/participants", roomId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_PARTICIPANT_ALREADY_EXISTS"));
    }

    @Test
    void mapsClosedRoomJoinToConflict() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(roomService.joinRoom(roomId, userId))
                .thenThrow(new RoomJoinNotAllowedException(roomId, RoomStatus.VOTING));

        mockMvc.perform(post("/api/rooms/{roomId}/participants", roomId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_JOIN_NOT_ALLOWED"));
    }

    @Test
    void mapsInvalidTransitionToConflict() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(roomService.changeRoomStatus(roomId, creatorId, RoomStatus.FINISHED))
                .thenThrow(new InvalidRoomStatusTransitionException(
                        roomId,
                        RoomStatus.CREATED,
                        RoomStatus.FINISHED
                ));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, creatorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "FINISHED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_STATUS_TRANSITION"));
    }

    @Test
    void mapsUnauthorizedStatusChangeToForbidden() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(roomService.changeRoomStatus(roomId, actorId, RoomStatus.VOTING))
                .thenThrow(new RoomActionNotAllowedException(roomId, actorId));

        mockMvc.perform(put("/api/rooms/{roomId}/status", roomId)
                        .header(USER_ID_HEADER, actorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "VOTING"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_ACTION_NOT_ALLOWED"));
    }

    @Test
    void mapsMissingUserHeaderToBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Friday movies"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Required request header is missing: X-User-Id"));
    }

    @Test
    void mapsMalformedUserHeaderToBadRequest() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header(USER_ID_HEADER, "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Friday movies"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for X-User-Id"));
    }

    @Test
    void mapsMalformedStatusToBadRequest() throws Exception {
        mockMvc.perform(put("/api/rooms/{roomId}/status", UUID.randomUUID())
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "UNKNOWN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }

    private RoomData roomData(UUID roomId, UUID creatorId, RoomStatus status) {
        RoomMemberData creator = member(creatorId, "Alice", "alice@example.com");
        return new RoomData(
                roomId,
                "Friday movies",
                creator,
                List.of(creator),
                status,
                CREATED_AT
        );
    }

    private RoomMemberData member(UUID id, String name, String email) {
        return new RoomMemberData(id, name, email);
    }
}
