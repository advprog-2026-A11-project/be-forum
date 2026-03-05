CREATE TABLE reactions
(
    id            UUID PRIMARY KEY,
    reaction_type VARCHAR(20)              NOT NULL,
    user_id       VARCHAR(255)             NOT NULL,
    message_id    UUID                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reactions_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
);

CREATE INDEX idx_reactions_message_id ON reactions (message_id);
CREATE INDEX idx_reactions_user_id ON reactions (user_id);
CREATE UNIQUE INDEX idx_reactions_unique ON reactions (message_id, user_id, reaction_type);
