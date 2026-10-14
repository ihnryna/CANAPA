package org.spring.canapa.backend.voting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ElementCollection
    @CollectionTable(
            name = "voting_result_winners",
            joinColumns = @JoinColumn(name = "voting_result_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_voting_result_winner",
                    columnNames = {"voting_result_id", "movie_id"}
            )
    )
    @Column(name = "movie_id", nullable = false)
    private List<Long> winnerMovieIds = new ArrayList<>();

    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @PrePersist
    protected void onCreate() {
        if (finishedAt == null) {
            finishedAt = LocalDateTime.now();
        }
    }
}