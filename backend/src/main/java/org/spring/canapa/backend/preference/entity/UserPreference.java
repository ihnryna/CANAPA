package org.spring.canapa.backend.preference.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Entity
@Table(
    name = "user_preferences",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_room_preference",
        columnNames = {"user_id", "room_id"}
    ),
    indexes = {
        @Index(name = "idx_preferences_room", columnList = "room_id"),
        @Index(name = "idx_preferences_user", columnList = "user_id")
    }
)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "preference_liked_genres",
        joinColumns = @JoinColumn(name = "preference_id")
    )
    @Column(name = "genre", nullable = false, length = 100)
    private Set<String> likedGenres = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "preference_disliked_genres",
        joinColumns = @JoinColumn(name = "preference_id")
    )
    @Column(name = "genre", nullable = false, length = 100)
    private Set<String> dislikedGenres = new HashSet<>();

    @Column
    private Double minRating;

    @Column
    private Integer maxRuntime;

    @Column(length = 10)
    private String preferredLanguage;

    @Column(columnDefinition = "TEXT")
    private String textPreference;

    public void setLikedGenres(Set<String> likedGenres) {
        this.likedGenres = likedGenres == null
            ? new HashSet<>()
            : new HashSet<>(likedGenres);
    }

    public void setDislikedGenres(Set<String> dislikedGenres) {
        this.dislikedGenres = dislikedGenres == null
            ? new HashSet<>()
            : new HashSet<>(dislikedGenres);
    }
}
