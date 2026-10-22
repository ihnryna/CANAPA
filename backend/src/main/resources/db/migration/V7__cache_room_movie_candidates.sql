CREATE TABLE room_movie_candidates (
    room_id UUID NOT NULL,
    movie_id BIGINT NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT pk_room_movie_candidates PRIMARY KEY (room_id, movie_id),
    CONSTRAINT uk_room_movie_candidates_position UNIQUE (room_id, position),
    CONSTRAINT ck_room_movie_candidates_position CHECK (position >= 0),
    CONSTRAINT fk_room_movie_candidates_room FOREIGN KEY (room_id)
        REFERENCES rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_room_movie_candidates_movie FOREIGN KEY (movie_id)
        REFERENCES movies (id) ON DELETE RESTRICT
);

CREATE INDEX ix_room_movie_candidates_movie_id
    ON room_movie_candidates (movie_id);
