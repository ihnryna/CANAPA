UPDATE rooms room
SET status = 'FINISHED'
WHERE EXISTS (
    SELECT 1
    FROM voting_results result
    WHERE result.room_id = room.id
);
