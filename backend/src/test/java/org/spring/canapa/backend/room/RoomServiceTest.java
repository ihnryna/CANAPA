package org.spring.canapa.backend.room;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spring.canapa.backend.room.dto.CreateRoomCommand;
import org.spring.canapa.backend.room.dto.RoomData;
import org.spring.canapa.backend.room.exception.InvalidRoomDataException;
import org.spring.canapa.backend.room.exception.InvalidRoomStatusTransitionException;
import org.spring.canapa.backend.room.exception.RoomActionNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomJoinNotAllowedException;
import org.spring.canapa.backend.room.exception.RoomNotFoundException;
import org.spring.canapa.backend.room.exception.RoomParticipantAlreadyExistsException;
import org.spring.canapa.backend.user.User;
import org.spring.canapa.backend.user.UserRepository;
import org.spring.canapa.backend.user.exception.UserNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void createsRoomWithNormalizedNameAndCreatorMembership() {
        User creator = persistedUser("Alice", "alice@example.com");
        UUID roomId = UUID.randomUUID();
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(roomRepository.saveAndFlush(any(Room.class)))
                .thenAnswer(invocation -> persistedRoom(invocation.getArgument(0), roomId));

        RoomData result = roomService.createRoom(
                new CreateRoomCommand("  Friday movies  ", creator.getId())
        );

        assertThat(result.id()).isEqualTo(roomId);
        assertThat(result.name()).isEqualTo("Friday movies");
        assertThat(result.creator().id()).isEqualTo(creator.getId());
        assertThat(result.participants()).containsExactly(result.creator());
        assertThat(result.status()).isEqualTo(RoomStatus.CREATED);
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void rejectsInvalidRoomName() {
        assertThatThrownBy(() -> roomService.createRoom(
                new CreateRoomCommand("   ", UUID.randomUUID())
        )).isInstanceOf(InvalidRoomDataException.class)
                .hasMessage("Invalid name: must not be blank");

        verify(roomRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void failsWhenCreatorDoesNotExist() {
        UUID creatorId = UUID.randomUUID();
        when(userRepository.findById(creatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(
                new CreateRoomCommand("Friday movies", creatorId)
        )).isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + creatorId);

        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void retrievesRoomWithParticipantsInStableOrder() {
        User creator = persistedUser("Zoe", "zoe@example.com");
        User alice = persistedUser("alice", "alice@example.com");
        User otherAlice = persistedUser("Alice", "alice.two@example.com");
        ReflectionTestUtils.setField(
                alice,
                "id",
                UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
        ReflectionTestUtils.setField(
                otherAlice,
                "id",
                UUID.fromString("00000000-0000-0000-0000-000000000002")
        );
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        room.addParticipant(alice);
        room.addParticipant(otherAlice);
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        RoomData result = roomService.getRoom(room.getId());

        assertThat(result.participants())
                .extracting(participant -> participant.name())
                .containsExactly("alice", "Alice", "Zoe");
    }

    @Test
    void failsWhenRoomDoesNotExist() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(roomId))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room not found: " + roomId);
    }

    @Test
    void joinsUserToCreatedRoom() {
        User creator = persistedUser("Alice", "alice@example.com");
        User participant = persistedUser("Bob", "bob@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(participant.getId())).thenReturn(Optional.of(participant));
        when(roomRepository.saveAndFlush(room)).thenReturn(room);

        RoomData result = roomService.joinRoom(room.getId(), participant.getId());

        assertThat(result.participants())
                .extracting(member -> member.id())
                .containsExactly(creator.getId(), participant.getId());
        verify(roomRepository).findByIdForUpdate(room.getId());
        verify(roomRepository).saveAndFlush(room);
    }

    @Test
    void rejectsDuplicateParticipant() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom(room.getId(), creator.getId()))
                .isInstanceOf(RoomParticipantAlreadyExistsException.class);

        verify(userRepository, never()).findById(any());
        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsJoinAfterVotingStarts() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        room.changeStatus(RoomStatus.VOTING);
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.joinRoom(room.getId(), UUID.randomUUID()))
                .isInstanceOf(RoomJoinNotAllowedException.class)
                .hasMessageContaining("while it is VOTING");

        verify(userRepository, never()).findById(any());
    }

    @Test
    void rejectsJoinForUnknownUser() {
        User creator = persistedUser("Alice", "alice@example.com");
        UUID participantId = UUID.randomUUID();
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(participantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.joinRoom(room.getId(), participantId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + participantId);
    }

    @Test
    void creatorAdvancesRoomThroughLifecycle() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(roomRepository.saveAndFlush(room)).thenReturn(room);

        RoomData voting = roomService.changeRoomStatus(
                room.getId(), creator.getId(), RoomStatus.VOTING
        );
        RoomData finished = roomService.changeRoomStatus(
                room.getId(), creator.getId(), RoomStatus.FINISHED
        );

        assertThat(voting.status()).isEqualTo(RoomStatus.VOTING);
        assertThat(finished.status()).isEqualTo(RoomStatus.FINISHED);
    }

    @Test
    void sameStatusChangeIsIdempotent() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        RoomData result = roomService.changeRoomStatus(
                room.getId(), creator.getId(), RoomStatus.CREATED
        );

        assertThat(result.status()).isEqualTo(RoomStatus.CREATED);
        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsStatusChangeByNonCreator() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        UUID actorId = UUID.randomUUID();
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.changeRoomStatus(
                room.getId(), actorId, RoomStatus.VOTING
        )).isInstanceOf(RoomActionNotAllowedException.class);

        verify(roomRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsSkippedStatusTransition() {
        User creator = persistedUser("Alice", "alice@example.com");
        Room room = persistedRoom(new Room("Friday movies", creator), UUID.randomUUID());
        when(roomRepository.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.changeRoomStatus(
                room.getId(), creator.getId(), RoomStatus.FINISHED
        )).isInstanceOf(InvalidRoomStatusTransitionException.class)
                .hasMessageContaining("from CREATED to FINISHED");

        verify(roomRepository, never()).saveAndFlush(any());
    }

    private User persistedUser(String name, String email) {
        User user = new User(name, email);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-10-07T12:00:00Z"));
        return user;
    }

    private Room persistedRoom(Room room, UUID id) {
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-10-07T12:00:00Z"));
        return room;
    }
}
