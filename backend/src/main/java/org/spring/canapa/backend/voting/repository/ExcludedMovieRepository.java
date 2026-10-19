
package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.ExcludedMovie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExcludedMovieRepository
        extends JpaRepository<ExcludedMovie, Long> {

    List<ExcludedMovie> findAllByRoom_Id(UUID roomId);

    Optional<ExcludedMovie> findByRoom_IdAndMovie_Id(
            UUID roomId,
            Long movieId
    );

    boolean existsByRoom_IdAndMovie_Id(UUID roomId, Long movieId);
}
