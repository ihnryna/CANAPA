
package org.spring.canapa.backend.voting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExcludedMovieRepository
        extends JpaRepository<ExcludedMovie, Long> {

    List<ExcludedMovie> findAllByRoomId(Long roomId);

    Optional<ExcludedMovie> findByRoomIdAndMovieId(
            Long roomId,
            Long movieId
    );

    boolean existsByRoomIdAndMovieId(
            Long roomId,
            Long movieId
    );
}
