
package org.spring.canapa.backend.voting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findAllByRoomId(Long roomId);

    List<Vote> findAllByUserIdAndRoomId(Long userId, Long roomId);

    Optional<Vote> findByUserIdAndRoomIdAndMovieId(
            Long userId,
            Long roomId,
            Long movieId
    );

    boolean existsByUserIdAndRoomIdAndMovieId(
            Long userId,
            Long roomId,
            Long movieId
    );

    long deleteByUserIdAndRoomIdAndMovieId(
            Long userId,
            Long roomId,
            Long movieId
    );

    long deleteAllByUserIdAndRoomId(Long userId, Long roomId);
}
