package org.spring.canapa.backend.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") UUID roomId);
}
