package org.spring.canapa.backend.room;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.user.User;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_participants",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_movie_candidates",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    @OrderColumn(name = "position")
    private List<Movie> movieCandidates = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "movie_candidates_cached_at")
    private Instant movieCandidatesCachedAt;

    protected Room() {
    }

    public Room(String name, User creator) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.creator = Objects.requireNonNull(creator, "creator must not be null");
        this.participants.add(creator);
        this.status = RoomStatus.CREATED;
    }

    @PrePersist
    private void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    void addParticipant(User participant) {
        participants.add(Objects.requireNonNull(participant, "participant must not be null"));
    }

    void changeStatus(RoomStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void markVotingFinished() {
        this.status = RoomStatus.FINISHED;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getCreator() {
        return creator;
    }

    public Set<User> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public List<Movie> getMovieCandidates() {
        return Collections.unmodifiableList(movieCandidates);
    }

    public void cacheMovieCandidates(List<Movie> movies) {
        if (movieCandidatesCachedAt != null) {
            return;
        }

        movieCandidates.addAll(
            Objects.requireNonNull(movies, "movies must not be null")
        );
        movieCandidatesCachedAt = Instant.now();
    }

    public boolean hasMovieCandidatesCache() {
        return movieCandidatesCachedAt != null;
    }

    public Instant getMovieCandidatesCachedAt() {
        return movieCandidatesCachedAt;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Room room)) {
            return false;
        }
        return id != null && id.equals(room.id);
    }

    @Override
    public int hashCode() {
        return Room.class.hashCode();
    }
}
