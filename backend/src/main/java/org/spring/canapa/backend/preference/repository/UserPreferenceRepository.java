package org.spring.canapa.backend.preference.repository;

import org.spring.canapa.backend.preference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserIdAndRoomId(UUID userId, UUID roomId);

    List<UserPreference> findAllByRoomId(UUID roomId);

    boolean existsByUserIdAndRoomId(UUID userId, UUID roomId);

    boolean existsByRoomIdAndUserIdIn(UUID roomId, Collection<UUID> userIds);

    void deleteByUserIdAndRoomId(UUID userId, UUID roomId);
}
