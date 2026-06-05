-- Invite tokens: one-time-use registration codes minted by an ADMIN.
-- The token value is a 256-bit URL-safe random string.
CREATE TABLE IF NOT EXISTS invite_tokens (
    id                UUID         PRIMARY KEY,
    token             VARCHAR(255) NOT NULL,
    email             VARCHAR(255) NOT NULL,
    intended_role     VARCHAR(32)  NOT NULL,
    cost_center_id    BIGINT,
    created_by_id     UUID         NOT NULL,
    created_at        TIMESTAMP    NOT NULL,
    expires_at        TIMESTAMP    NOT NULL,
    used_at           TIMESTAMP,
    used_by_user_id   UUID,
    revoked           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_invite_tokens_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_invite_token ON invite_tokens (token);
CREATE INDEX IF NOT EXISTS idx_invite_email ON invite_tokens (email);
