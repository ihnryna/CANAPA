package org.spring.canapa.backend.preference.repository;

import org.spring.canapa.backend.preference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserIdAndRoomId(Long userId, Long roomId);

    List<UserPreference> findAllByRoomId(Long roomId);

    boolean existsByUserIdAndRoomId(Long userId, Long roomId);

    void deleteByUserIdAndRoomId(Long userId, Long roomId);
}
