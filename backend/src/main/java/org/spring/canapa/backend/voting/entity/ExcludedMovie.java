
package org.spring.canapa.backend.voting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "excluded_movies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_excluded_movie_room_movie",
                columnNames = {"room_id", "movie_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ExcludedMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "excluded_by_user_id", nullable = false)
    private UUID excludedByUserId;

    @Column(name = "excluded_at", nullable = false, updatable = false)
    private LocalDateTime excludedAt;

    @PrePersist
    protected void onCreate() {
        if (excludedAt == null) {
            excludedAt = LocalDateTime.now();
        }
    }
}
