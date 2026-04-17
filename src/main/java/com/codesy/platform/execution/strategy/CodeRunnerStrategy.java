package com.codesy.platform.execution.strategy;

import com.codesy.platform.execution.api.dto.JudgeResult;
import com.codesy.platform.problem.domain.TestCase;
import com.codesy.platform.submission.domain.ProgrammingLanguage;
import com.codesy.platform.submission.domain.Submission;

import java.util.List;

public interface CodeRunnerStrategy {

    ProgrammingLanguage supports();

    JudgeResult judge(Submission submission, List<TestCase> testCases);
}