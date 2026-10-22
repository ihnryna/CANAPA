ALTER TABLE rooms
    ADD COLUMN movie_candidates_cached_at TIMESTAMP WITH TIME ZONE;

UPDATE rooms room
SET movie_candidates_cached_at = CURRENT_TIMESTAMP
WHERE EXISTS (
    SELECT 1
    FROM room_movie_candidates candidate
    WHERE candidate.room_id = room.id
);
