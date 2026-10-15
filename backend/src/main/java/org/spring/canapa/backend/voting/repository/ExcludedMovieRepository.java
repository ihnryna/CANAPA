
package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.ExcludedMovie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExcludedMovieRepository
        extends JpaRepository<ExcludedMovie, Long> {

    List<ExcludedMovie> findAllByRoomId(UUID roomId);

    Optional<ExcludedMovie> findByRoomIdAndMovieId(
            UUID roomId,
            Long movieId
    );

    boolean existsByRoomIdAndMovieId(
            UUID roomId,
            Long movieId
    );
}
