
package org.spring.canapa.backend.voting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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

    @Column(name = "winner_movie_id")
    private Long winnerMovieId;

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
