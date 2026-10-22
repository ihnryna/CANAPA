
package org.spring.canapa.backend.voting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.user.User;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "excluded_by_user_id", nullable = false)
    private User excludedBy;

    @Column(name = "excluded_at", nullable = false, updatable = false)
    private LocalDateTime excludedAt;

    public ExcludedMovie(Room room, Movie movie, User excludedBy) {
        this.room = room;
        this.movie = movie;
        this.excludedBy = excludedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (excludedAt == null) {
            excludedAt = LocalDateTime.now();
        }
    }
}
