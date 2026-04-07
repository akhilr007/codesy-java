-- ===========================
-- Problems Table
-- ===========================
CREATE TABLE IF NOT EXISTS problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,

    difficulty VARCHAR(32) NOT NULL,
    CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

-- Index for querying by slug quickly
CREATE UNIQUE INDEX IF NOT EXISTS idx_problems_slug ON problems(slug);

-- ===========================
-- Problem Tags Table
-- ===========================
CREATE TABLE IF NOT EXISTS problem_tags (
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag)
);

-- Optional index for tag searches
CREATE INDEX IF NOT EXISTS idx_problem_tags_tag ON problem_tags(tag);

-- ===========================
-- Problem Versions Table
-- ===========================
CREATE TABLE IF NOT EXISTS problem_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,

    version_number INTEGER NOT NULL,

    statement TEXT NOT NULL,
    input_format TEXT,
    output_format TEXT,
    constraints_text TEXT,

    time_limit_ms INTEGER NOT NULL,
    memory_limit_mb INTEGER NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_problem_version UNIQUE (problem_id, version_number)
    );

-- Only ONE active version per problem
CREATE UNIQUE INDEX idx_one_active_version_per_problem
    ON problem_versions(problem_id)
    WHERE active = TRUE;


-- ===========================
-- Test Cases Table
-- ===========================
CREATE TABLE IF NOT EXISTS test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()

    problem_version_id UUID NOT NULL REFERENCES problem_versions(id) ON DELETE CASCADE,

    ordinal INTEGER NOT NULL,

    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,

    visibility VARCHAR(32) NOT NULL,
    CHECK (visibility IN ('SAMPLE', 'HIDDEN'))
);

-- Index for querying test cases by version and order
CREATE INDEX IF NOT EXISTS idx_test_cases_version_ordinal
    ON test_cases(problem_version_id, ordinal);

-- Index for quickly filtering sample/hidden cases
CREATE INDEX IF NOT EXISTS idx_test_cases_visibility
    ON test_cases(problem_version_id, visibility);