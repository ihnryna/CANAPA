package org.spring.canapa.backend.room.api;

import org.spring.canapa.backend.room.RoomService;
import org.spring.canapa.backend.room.api.dto.ChangeRoomStatusRequest;
import org.spring.canapa.backend.room.api.dto.CreateRoomRequest;
import org.spring.canapa.backend.room.api.dto.RoomMemberResponse;
import org.spring.canapa.backend.room.api.dto.RoomResponse;
import org.spring.canapa.backend.room.dto.CreateRoomCommand;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.dto.RoomMemberData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestHeader(USER_ID_HEADER) UUID creatorId,
            @RequestBody CreateRoomRequest request
    ) {
        RoomResponse response = toResponse(roomService.createRoom(
                new CreateRoomCommand(request.name(), creatorId)
        ));

        return ResponseEntity
                .created(URI.create("/api/rooms/" + response.id()))
                .body(response);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable UUID roomId) {
        return toResponse(roomService.getRoom(roomId));
    }

    @PostMapping("/{roomId}/participants")
    public RoomResponse joinRoom(
            @PathVariable UUID roomId,
            @RequestHeader(USER_ID_HEADER) UUID userId
    ) {
        return toResponse(roomService.joinRoom(roomId, userId));
    }

    @PutMapping("/{roomId}/status")
    public RoomResponse changeRoomStatus(
            @PathVariable UUID roomId,
            @RequestHeader(USER_ID_HEADER) UUID actorUserId,
            @RequestBody ChangeRoomStatusRequest request
    ) {
        return toResponse(roomService.changeRoomStatus(
                roomId,
                actorUserId,
                request.status()
        ));
    }

    private RoomResponse toResponse(RoomData room) {
        return new RoomResponse(
                room.id(),
                room.name(),
                toResponse(room.creator()),
                room.participants().stream().map(this::toResponse).toList(),
                room.status(),
                room.createdAt()
        );
    }

    private RoomMemberResponse toResponse(RoomMemberData member) {
        return new RoomMemberResponse(member.id(), member.name());
    }
}
