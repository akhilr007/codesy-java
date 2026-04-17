CREATE TABLE IF NOT EXISTS leaderboard_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES app_users(id),
    username VARCHAR(80) NOT NULL,
    scope VARCHAR(64) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    accepted_submissions INTEGER NOT NULL DEFAULT 0,
    last_submission_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_leaderboard_user_scope
    ON leaderboard_entries(user_id, scope);

CREATE INDEX idx_leaderboard_scope_rank
ON leaderboard_entries (
    scope,
    score DESC,
    accepted_submissions DESC,
    updated_at ASC,
    username ASC
);