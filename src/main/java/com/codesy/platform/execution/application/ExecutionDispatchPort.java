package com.codesy.platform.execution.application;

import com.codesy.platform.execution.domain.SubmissionQueuedPayload;

public interface ExecutionDispatchPort {

    void dispatch(SubmissionQueuedPayload payload);
}