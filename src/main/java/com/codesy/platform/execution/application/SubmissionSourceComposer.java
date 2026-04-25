package com.codesy.platform.execution.application;

import com.codesy.platform.problem.domain.ProblemCodeTemplateConstants;
import com.codesy.platform.problem.domain.ProblemVersion;
import com.codesy.platform.submission.domain.ProgrammingLanguage;
import com.codesy.platform.submission.domain.Submission;
import org.springframework.stereotype.Component;

@Component
public class SubmissionSourceComposer {

    public String compose(Submission submission) {
        String sourceCode = submission.getSourceCode();
        if (sourceCode == null) {
            return "";
        }

        String executionTemplate = executionTemplateFor(submission.getProblemVersion(), submission.getLanguage());
        if (executionTemplate == null || executionTemplate.isBlank()) {
            return sourceCode;
        }

        if (!executionTemplate.contains(ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER)) {
            throw new IllegalStateException(
                    "Execution template for problem "
                            + submission.getProblem().getSlug()
                            + " and language "
                            + submission.getLanguage()
                            + " is missing "
                            + ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER
            );
        }

        return executionTemplate.replace(ProblemCodeTemplateConstants.USER_CODE_PLACEHOLDER, sourceCode);
    }

    private String executionTemplateFor(ProblemVersion problemVersion,
                                        ProgrammingLanguage language) {
        return switch (language) {
            case JAVA_21 -> problemVersion.getJavaExecutionTemplate();
            case PYTHON_3 -> problemVersion.getPythonExecutionTemplate();
            case CPP_17 -> problemVersion.getCppExecutionTemplate();
        };
    }
}
