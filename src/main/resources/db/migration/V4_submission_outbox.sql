CREATE TABLE IF NOT EXISTS submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL REFERENCES app_users(id),
    problem_id UUID NOT NULL REFERENCES problems(id),
    problem_version_id UUID NOT NULL REFERENCES problem_versions(id),
    language VARCHAR(32) NOT NULL,
    source_code TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    correlation_id UUID NOT NULL,
    attempt_no INTEGER NOT NULL DEFAULT 1,
    queued_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_submission_language
    CHECK (language IN ('JAVA_21', 'PYTHON_3', 'CPP_17')),

    CONSTRAINT chk_submission_status
    CHECK (status IN ('CREATED', 'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_submissions_user_created
    ON submissions(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_submissions_problem_status
    ON submissions(problem_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS idx_submissions_correlation_id
    ON submissions(correlation_id);

CREATE TABLE IF NOT EXISTS submission_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    submission_id UUID NOT NULL UNIQUE REFERENCES submissions(id) ON DELETE CASCADE,
    passed_tests INTEGER NOT NULL,
    total_tests INTEGER NOT NULL,
    verdict VARCHAR(64) NOT NULL,
    runtime_ms BIGINT,
    memory_kb BIGINT,
    execution_log TEXT,
    compiler_output TEXT,

    CONSTRAINT chk_submission_verdict
    CHECK (verdict IN (
       'ACCEPTED',
       'WRONG_ANSWER',
       'TIME_LIMIT_EXCEEDED',
       'MEMORY_LIMIT_EXCEEDED',
       'RUNTIME_ERROR',
       'COMPILATION_ERROR',
       'INTERNAL_ERROR'
    ))
);

CREATE TABLE IF NOT EXISTS submission_test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    submission_result_id UUID NOT NULL REFERENCES submission_results(id) ON DELETE CASCADE,
    test_case_id UUID NOT NULL,
    test_case_ordinal INTEGER NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    verdict VARCHAR(64) NOT NULL,
    input_snapshot TEXT NOT NULL,
    expected_output_snapshot TEXT NOT NULL,
    actual_output TEXT,
    runtime_ms BIGINT,
    memory_kb BIGINT,
    message TEXT,

    CONSTRAINT chk_testcase_visibility
    CHECK (visibility IN ('SAMPLE', 'HIDDEN')),

    CONSTRAINT chk_testcase_result_verdict
    CHECK (verdict IN (
       'PASSED',
       'WRONG_ANSWER',
       'TIME_LIMIT_EXCEEDED',
       'MEMORY_LIMIT_EXCEEDED',
       'RUNTIME_ERROR',
       'NOT_RUN'
    ))
);

CREATE INDEX IF NOT EXISTS idx_submission_test_results_submission_result
    ON submission_test_results(submission_result_id, test_case_ordinal);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    processed_at TIMESTAMPTZ,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON outbox_events(status, created_at);