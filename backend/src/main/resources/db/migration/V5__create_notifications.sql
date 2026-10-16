CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    room_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_notifications_message_not_blank CHECK (char_length(btrim(message)) > 0),
    CONSTRAINT ck_notifications_type CHECK (
        type IN ('VOTING_STARTED', 'VOTING_ENDED', 'VOTE_CAST', 'MOVIE_ADDED', 'MOVIE_REMOVED', 'RESULTS_READY')
    ),
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_notifications_sent_at CHECK (
        (status = 'SENT' AND sent_at IS NOT NULL) OR
        (status <> 'SENT' AND sent_at IS NULL)
    ),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
);

CREATE INDEX ix_notifications_recipient_status ON notifications (recipient_user_id, status);
CREATE INDEX ix_notifications_room_id ON notifications (room_id);
