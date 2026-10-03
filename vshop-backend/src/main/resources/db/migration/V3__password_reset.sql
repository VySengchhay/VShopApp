-- One row per "forgot password" request.
-- The code itself is never stored, only its BCrypt hash.
CREATE TABLE password_reset_otps (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash   VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,          -- created_at + 5 minutes
    attempts    INT          NOT NULL DEFAULT 0, -- wrong tries so far (max 5)
    verified_at TIMESTAMPTZ,                     -- when the right code was entered
    used_at     TIMESTAMPTZ,                     -- when the password was changed (or the code was replaced)
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_password_reset_otps_user ON password_reset_otps(user_id);
