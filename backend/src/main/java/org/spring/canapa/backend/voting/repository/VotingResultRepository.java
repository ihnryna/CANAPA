
package org.spring.canapa.backend.voting.repository;

import org.spring.canapa.backend.voting.entity.VotingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VotingResultRepository
        extends JpaRepository<VotingResult, Long> {

    Optional<VotingResult> findByRoomId(Long roomId);

    boolean existsByRoomId(Long roomId);
}
