
package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findAllByRoom_Id(UUID roomId);

    List<Vote> findAllByUser_IdAndRoom_Id(UUID userId, UUID roomId);

    Optional<Vote> findByUser_IdAndRoom_IdAndMovie_Id(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    boolean existsByUser_IdAndRoom_IdAndMovie_Id(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    void deleteByUser_IdAndRoom_IdAndMovie_Id(
            UUID userId,
            UUID roomId,
            Long movieId
    );

    void deleteAllByUser_IdAndRoom_Id(UUID userId, UUID roomId);
}
