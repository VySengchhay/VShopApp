-- ============================================================
-- Auth v2: short-lived access JWT + rotating refresh tokens,
-- password reset with email + 6-digit code (request / verify / confirm).
-- ============================================================

-- One row per issued refresh token. Only the SHA-256 hash (Base64, 44 chars) is stored;
-- the raw token is returned to the app once. /refresh revokes the old row and inserts a new one.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expiry_date);   -- daily clean-up job

-- password_reset_otps (V3) already has everything the new flow needs:
--   user_id    -> the account the email belongs to (codes are only created for registered emails)
--   code_hash  -> BCrypt hash of the 6-digit code
--   attempts   -> wrong tries (max app.otp.max-verify-attempts)
--   used_at    -> "used" (password changed, or replaced by a newer code)
--   verified_at-> last successful /password-reset/verify (verify no longer consumes the code)
-- The rate limit counts codes per user in the last hour, so index (user_id, created_at).
DROP INDEX idx_password_reset_otps_user;
CREATE INDEX idx_password_reset_otps_user_created ON password_reset_otps(user_id, created_at);

COMMENT ON COLUMN password_reset_otps.expires_at  IS 'created_at + app.otp.expiry-minutes (default 10)';
COMMENT ON COLUMN password_reset_otps.verified_at IS 'last time the right code was checked with /password-reset/verify';

-- users.full_name stays NOT NULL: register no longer sends a name, so the backend fills it
-- with the local part of the email (e.g. "vy" for vy@example.com). PUT /api/me can change it.
-- It is used as the payer name sent to PayWay.
