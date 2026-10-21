package org.spring.canapa.backend.voting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.room.Room;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "voting_results",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_voting_result_room",
        columnNames = "room_id"
    )
)
@Getter
@Setter
@NoArgsConstructor
public class VotingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToMany
    @JoinTable(
        name = "voting_result_winners",
        joinColumns = @JoinColumn(name = "voting_result_id"),
        inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    private List<Movie> winners = new ArrayList<>();

    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    public VotingResult(Room room, List<Movie> winners, int voteCount) {
        this.room = room;
        this.winners = new ArrayList<>(winners);
        this.voteCount = voteCount;
    }

    @PrePersist
    protected void onCreate() {
        if (finishedAt == null) {
            finishedAt = LocalDateTime.now();
        }
    }
}
