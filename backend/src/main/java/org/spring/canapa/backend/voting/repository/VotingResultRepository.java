
package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.VotingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VotingResultRepository
        extends JpaRepository<VotingResult, Long> {

    Optional<VotingResult> findByRoomId(UUID roomId);

    boolean existsByRoomId(UUID roomId);
}
