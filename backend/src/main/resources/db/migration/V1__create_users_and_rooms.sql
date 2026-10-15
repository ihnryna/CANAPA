CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_name_not_blank CHECK (char_length(btrim(name)) > 0),
    CONSTRAINT ck_users_email_not_blank CHECK (char_length(btrim(email)) > 0)
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rooms_name_not_blank CHECK (char_length(btrim(name)) > 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('CREATED', 'VOTING', 'FINISHED')),
    CONSTRAINT fk_rooms_creator FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX ix_rooms_creator_id ON rooms (creator_id);

CREATE TABLE room_participants (
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_room_participants PRIMARY KEY (room_id, user_id),
    CONSTRAINT fk_room_participants_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_participants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX ix_room_participants_user_id ON room_participants (user_id);
