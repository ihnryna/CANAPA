package org.spring.canapa.backend.room;

import org.spring.canapa.backend.room.dto.CreateRoomCommand;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.dto.RoomMemberData;
import org.spring.canapa.backend.room.exception.InvalidRoomDataException;
import org.spring.canapa.backend.room.exception.InvalidRoomStatusTransitionException;
import org.spring.canapa.backend.room.exception.RoomActionNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomJoinNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomNotFoundException;
import org.spring.canapa.backend.room.exception.RoomParticipantAlreadyExistsException;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private static final int MAX_NAME_LENGTH = 100;
    private static final Comparator<User> PARTICIPANT_ORDER = Comparator
            .comparing(User::getName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(User::getId);

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RoomData createRoom(CreateRoomCommand command) {
        if (command == null) {
            throw new InvalidRoomDataException("request", "must not be null");
        }

        String name = normalizeAndValidateName(command.name());
        User creator = findUser(command.creatorId(), "creatorId");
        return toData(roomRepository.saveAndFlush(new Room(name, creator)));
    }

    @Transactional(readOnly = true)
    public RoomData getRoom(UUID roomId) {
        return toData(findRoom(roomId));
    }

    @Transactional
    public RoomData joinRoom(UUID roomId, UUID userId) {
        validateId(userId, "userId");
        Room room = findRoomForUpdate(roomId);

        if (room.getStatus() != RoomStatus.CREATED) {
            throw new RoomJoinNotAllowedException(room.getId(), room.getStatus());
        }
        if (hasParticipant(room, userId)) {
            throw new RoomParticipantAlreadyExistsException(room.getId(), userId);
        }

        User participant = findUser(userId, "userId");
        room.addParticipant(participant);
        try {
            return toData(roomRepository.saveAndFlush(room));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomParticipantAlreadyExistsException(room.getId(), userId, exception);
        }
    }

    @Transactional
    public RoomData changeRoomStatus(UUID roomId, UUID actorUserId, RoomStatus targetStatus) {
        validateId(actorUserId, "actorUserId");
        if (targetStatus == null) {
            throw new InvalidRoomDataException("targetStatus", "must not be null");
        }

        Room room = findRoomForUpdate(roomId);
        if (!room.getCreator().getId().equals(actorUserId)) {
            throw new RoomActionNotAllowedException(room.getId(), actorUserId);
        }
        if (room.getStatus() == targetStatus) {
            return toData(room);
        }
        if (!isAllowedTransition(room.getStatus(), targetStatus)) {
            throw new InvalidRoomStatusTransitionException(room.getId(), room.getStatus(), targetStatus);
        }

        room.changeStatus(targetStatus);
        return toData(roomRepository.saveAndFlush(room));
    }

    private Room findRoom(UUID roomId) {
        validateId(roomId, "roomId");
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private Room findRoomForUpdate(UUID roomId) {
        validateId(roomId, "roomId");
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    private User findUser(UUID userId, String field) {
        validateId(userId, field);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validateId(UUID id, String field) {
        if (id == null) {
            throw new InvalidRoomDataException(field, "must not be null");
        }
    }

    private String normalizeAndValidateName(String value) {
        if (value == null) {
            throw new InvalidRoomDataException("name", "must not be null");
        }

        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new InvalidRoomDataException("name", "must not be blank");
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new InvalidRoomDataException("name", "must not exceed 100 characters");
        }
        return normalized;
    }

    private boolean hasParticipant(Room room, UUID userId) {
        return room.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));
    }

    private boolean isAllowedTransition(RoomStatus currentStatus, RoomStatus targetStatus) {
        return currentStatus == RoomStatus.CREATED && targetStatus == RoomStatus.VOTING
                || currentStatus == RoomStatus.VOTING && targetStatus == RoomStatus.FINISHED;
    }

    private RoomData toData(Room room) {
        List<RoomMemberData> participants = room.getParticipants().stream()
                .sorted(PARTICIPANT_ORDER)
                .map(this::toMemberData)
                .toList();

        return new RoomData(
                room.getId(),
                room.getName(),
                toMemberData(room.getCreator()),
                participants,
                room.getStatus(),
                room.getCreatedAt()
        );
    }

    private RoomMemberData toMemberData(User user) {
        return new RoomMemberData(user.getId(), user.getName(), user.getEmail());
    }
}
