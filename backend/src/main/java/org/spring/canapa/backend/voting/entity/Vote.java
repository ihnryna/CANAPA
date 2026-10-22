
package org.spring.canapa.backend.voting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.spring.canapa.backend.movie.entity.Movie;
import org.spring.canapa.backend.room.Room;
import org.spring.canapa.backend.user.User;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_vote_user_room_movie",
        columnNames = {"user_id", "room_id", "movie_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Vote(User user, Room room, Movie movie) {
        this.user = user;
        this.room = room;
        this.movie = movie;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
