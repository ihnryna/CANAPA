CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT uk_invitations_code UNIQUE (code),
    CONSTRAINT ck_invitations_code_not_blank CHECK (char_length(btrim(code)) > 0),
    CONSTRAINT ck_invitations_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_invitations_status CHECK (status IN ('ACTIVE', 'EXPIRED')),
    CONSTRAINT fk_invitations_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX ix_invitations_room_id ON invitations (room_id);
CREATE INDEX ix_invitations_created_by_user_id ON invitations (created_by_user_id);
CREATE INDEX ix_invitations_status_expires_at ON invitations (status, expires_at);
