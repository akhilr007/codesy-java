package com.codesy.platform.submission.domain;

public enum SubmissionVerdict {
    ACCEPTED,
    WRONG_ANSWER,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED,
    RUNTIME_ERROR,
    COMPILATION_ERROR,
    INTERNAL_ERROR
}