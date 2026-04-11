CREATE TABLE IF NOT EXISTS problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS problem_tags (
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (problem_id, tag)
);

CREATE TABLE IF NOT EXISTS problem_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    statement TEXT NOT NULL,
    input_format TEXT,
    output_format TEXT,
    constraints_text TEXT,
    time_limit_ms INTEGER NOT NULL,
    memory_limit_mb INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_problem_versions_problem_version
    ON problem_versions(problem_id, version_number);

CREATE TABLE IF NOT EXISTS test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    problem_version_id UUID NOT NULL REFERENCES problem_versions(id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    visibility VARCHAR(32) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_test_cases_problem_version_ordinal
    ON test_cases(problem_version_id, ordinal);