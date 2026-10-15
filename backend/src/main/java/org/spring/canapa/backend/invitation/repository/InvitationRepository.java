
package org.spring.canapa.backend.invitation.repository;

import org.spring.canapa.backend.invitation.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository
        extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByCode(String code);

    boolean existsByCode(String code);
}
