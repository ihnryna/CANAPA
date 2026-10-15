package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findAllByRoomId(UUID roomId);

    List<Vote> findAllByUserIdAndRoomId(UUID userId, UUID roomId);

    Optional<Vote> findByUserIdAndRoomIdAndMovieId(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    boolean existsByUserIdAndRoomIdAndMovieId(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    void deleteByUserIdAndRoomIdAndMovieId(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    void deleteAllByUserIdAndRoomId(UUID userId, UUID roomId);
}