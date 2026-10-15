package org.spring.canapa.backend.movie.repository;

import org.spring.canapa.backend.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByTmdbId(Long tmdbId);

    Optional<Movie> findByImdbId(String imdbId);

    List<Movie> findByTmdbIdIn(Collection<Long> tmdbIds);

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
        SELECT DISTINCT m FROM Movie m
        LEFT JOIN m.genres g
        WHERE (:minRating IS NULL OR m.rating >= :minRating)
          AND (:maxRuntime IS NULL OR m.runtime <= :maxRuntime)
          AND (:language IS NULL OR m.originalLanguage = :language)
          AND (:excludedGenresEmpty = true OR g NOT IN :excludedGenres)
        ORDER BY m.rating DESC NULLS LAST
        """)
    List<Movie> findMatchingMovies(
            @Param("minRating") Double minRating,
            @Param("maxRuntime") Integer maxRuntime,
            @Param("language") String language,
            @Param("excludedGenresEmpty") boolean excludedGenresEmpty,
            @Param("excludedGenres") Collection<String> excludedGenres,
            Pageable pageable
    );
}
