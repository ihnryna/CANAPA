package org.spring.canapa.backend.movie.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(
    name = "movies",
    indexes = {
        @Index(name = "idx_movies_title", columnList = "title"),
        @Index(name = "idx_movies_tmdb_id", columnList = "tmdb_id", unique = true),
        @Index(name = "idx_movies_imdb_id", columnList = "imdb_id")
    }
)
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(name = "imdb_id", length = 20)
    private String imdbId; // the id for OMDb movie, it doesn't use its own

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "movie_genres",
        joinColumns = @JoinColumn(name = "movie_id")
    )
    @Column(name = "genre", nullable = false, length = 100)
    private Set<String> genres = new HashSet<>();

    @Column
    private Double rating;

    @Column
    private Integer runtime;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(length = 10)
    private String originalLanguage;

    @Column(length = 300)
    private String director;

    @Column(columnDefinition = "TEXT")
    private String actors;

    @Column(columnDefinition = "TEXT")
    private String writers;

    @Column(length = 30)
    private String rated;

    @Column
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
