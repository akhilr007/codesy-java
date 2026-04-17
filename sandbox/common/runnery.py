#!/usr/bin/env python3

import json
import os
import subprocess
import sys
import time
from pathlib import Path


REQUEST_FILE = Path(os.environ.get("CODESY_REQUEST_FILE", "/workspace/request.json"))
SOURCE_FILE = Path(os.environ.get("CODESY_SOURCE_FILE", "/workspace/main.txt"))
COMPILE_OUTPUT_FILE = Path(os.environ.get("CODESY_COMPILE_OUTPUT_FILE", "/workspace/compile-output.txt"))
STDOUT_FILE = Path(os.environ.get("CODESY_STDOUT_FILE", "/workspace/stdout.txt"))
STDERR_FILE = Path(os.environ.get("CODESY_STDERR_FILE", "/workspace/stderr.txt"))
RESULT_FILE = Path(os.environ.get("CODESY_RESULT_FILE", "/workspace/result.json"))
COMPILE_TIMEOUT_MS = int(os.environ.get("CODESY_COMPILE_TIMEOUT_MS", "10000"))
RUN_TIMEOUT_MS = int(os.environ.get("CODESY_RUN_TIMEOUT_MS", "2000"))


def normalize_output(value: str | None) -> str:
    if value is None:
        return ""
    return "\n".join(line.rstrip() for line in value.strip().splitlines()).strip()


def write_text(path: Path, value: str) -> None:
    path.write_text(value, encoding="utf-8")


def write_result(payload: dict) -> None:
    RESULT_FILE.write_text(json.dumps(payload), encoding="utf-8")


def command_for_language(language: str, workspace: Path) -> tuple[list[str] | None, list[str]]:
    if language == "JAVA_21":
        return ["javac", SOURCE_FILE.name], ["java", "Main"]
    if language == "PYTHON_3":
        return None, ["python3", SOURCE_FILE.name]
    if language == "CPP_17":
        return ["g++", "-std=c++17", "-O2", SOURCE_FILE.name, "-o", "main"], [str(workspace / "main")]
    raise ValueError(f"Unsupported language {language}")


def run_process(command: list[str], workspace: Path, timeout_ms: int, input_data: str | None = None) -> tuple[subprocess.CompletedProcess[str], int]:
    started_at = time.monotonic()
    completed = subprocess.run(
        command,
        cwd=workspace,
        input=input_data,
        text=True,
        capture_output=True,
        timeout=timeout_ms / 1000.0,
        check=False,
    )
    duration_ms = int((time.monotonic() - started_at) * 1000)
    return completed, duration_ms


def build_not_run_cases(test_cases: list[dict], start_index: int) -> list[dict]:
    results = []
    for test_case in test_cases[start_index:]:
        results.append(
            {
                "testCaseId": test_case["testCaseId"],
                "ordinal": test_case["ordinal"],
                "visibility": test_case["visibility"],
                "verdict": "NOT_RUN",
                "runtimeMs": None,
                "memoryKb": None,
                "message": "Execution stopped after an earlier failure",
                "actualOutput": None,
            }
        )
    return results


def main() -> int:
    language = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("CODESY_LANGUAGE", "")
    workspace = REQUEST_FILE.parent
    request = json.loads(REQUEST_FILE.read_text(encoding="utf-8"))
    test_cases = request.get("testCases", [])

    compile_command, run_command = command_for_language(language, workspace)

    compile_stdout = ""
    compile_stderr = ""
    if compile_command is not None:
        try:
            compile_process, _ = run_process(compile_command, workspace, COMPILE_TIMEOUT_MS)
            compile_stdout = compile_process.stdout or ""
            compile_stderr = compile_process.stderr or ""
            write_text(COMPILE_OUTPUT_FILE, (compile_stdout + "\n" + compile_stderr).strip())
            if compile_process.returncode != 0:
                write_result(
                    {
                        "verdict": "COMPILATION_ERROR",
                        "runtimeMs": None,
                        "memoryKb": None,
                        "executionLog": f"{language} compilation failed",
                        "compilerOutput": (compile_stdout + "\n" + compile_stderr).strip(),
                        "stdout": "",
                        "stderr": compile_stderr,
                        "testCaseResults": [
                            {
                                "testCaseId": test_case["testCaseId"],
                                "ordinal": test_case["ordinal"],
                                "visibility": test_case["visibility"],
                                "verdict": "NOT_RUN",
                                "runtimeMs": None,
                                "memoryKb": None,
                                "message": "Compilation failed before this test could run",
                                "actualOutput": None,
                            }
                            for test_case in test_cases
                        ],
                    }
                )
                return 0
        except subprocess.TimeoutExpired:
            write_text(COMPILE_OUTPUT_FILE, "Compilation timed out")
            write_result(
                {
                    "verdict": "COMPILATION_ERROR",
                    "runtimeMs": None,
                    "memoryKb": None,
                    "executionLog": f"{language} compilation timed out",
                    "compilerOutput": "Compilation timed out",
                    "stdout": "",
                    "stderr": "",
                    "testCaseResults": [
                        {
                            "testCaseId": test_case["testCaseId"],
                            "ordinal": test_case["ordinal"],
                            "visibility": test_case["visibility"],
                            "verdict": "NOT_RUN",
                            "runtimeMs": None,
                            "memoryKb": None,
                            "message": "Compilation timed out before this test could run",
                            "actualOutput": None,
                        }
                        for test_case in test_cases
                    ],
                }
            )
            return 0

    overall_verdict = "ACCEPTED"
    total_runtime_ms = 0
    combined_stdout: list[str] = []
    combined_stderr: list[str] = []
    case_results: list[dict] = []

    for index, test_case in enumerate(test_cases):
        try:
            execution, runtime_ms = run_process(run_command, workspace, RUN_TIMEOUT_MS, test_case.get("inputData", ""))
        except subprocess.TimeoutExpired:
            case_results.append(
                {
                    "testCaseId": test_case["testCaseId"],
                    "ordinal": test_case["ordinal"],
                    "visibility": test_case["visibility"],
                    "verdict": "TIME_LIMIT_EXCEEDED",
                    "runtimeMs": RUN_TIMEOUT_MS,
                    "memoryKb": None,
                    "message": "Execution exceeded the allowed time limit",
                    "actualOutput": None,
                }
            )
            case_results.extend(build_not_run_cases(test_cases, index + 1))
            overall_verdict = "TIME_LIMIT_EXCEEDED"
            total_runtime_ms += RUN_TIMEOUT_MS
            break

        stdout = execution.stdout or ""
        stderr = execution.stderr or ""
        combined_stdout.append(stdout)
        combined_stderr.append(stderr)
        total_runtime_ms += runtime_ms

        if execution.returncode == 137:
            case_results.append(
                {
                    "testCaseId": test_case["testCaseId"],
                    "ordinal": test_case["ordinal"],
                    "visibility": test_case["visibility"],
                    "verdict": "MEMORY_LIMIT_EXCEEDED",
                    "runtimeMs": runtime_ms,
                    "memoryKb": None,
                    "message": "Execution exceeded the allowed memory limit",
                    "actualOutput": None,
                }
            )
            case_results.extend(build_not_run_cases(test_cases, index + 1))
            overall_verdict = "MEMORY_LIMIT_EXCEEDED"
            break

        if execution.returncode != 0:
            case_results.append(
                {
                    "testCaseId": test_case["testCaseId"],
                    "ordinal": test_case["ordinal"],
                    "visibility": test_case["visibility"],
                    "verdict": "RUNTIME_ERROR",
                    "runtimeMs": runtime_ms,
                    "memoryKb": None,
                    "message": "Unhandled runtime exception during execution",
                    "actualOutput": None,
                }
            )
            case_results.extend(build_not_run_cases(test_cases, index + 1))
            overall_verdict = "RUNTIME_ERROR"
            break

        actual_output = normalize_output(stdout)
        expected_output = normalize_output(test_case.get("expectedOutput"))
        if actual_output == expected_output:
            case_results.append(
                {
                    "testCaseId": test_case["testCaseId"],
                    "ordinal": test_case["ordinal"],
                    "visibility": test_case["visibility"],
                    "verdict": "PASSED",
                    "runtimeMs": runtime_ms,
                    "memoryKb": None,
                    "message": "Test passed",
                    "actualOutput": actual_output,
                }
            )
        else:
            case_results.append(
                {
                    "testCaseId": test_case["testCaseId"],
                    "ordinal": test_case["ordinal"],
                    "visibility": test_case["visibility"],
                    "verdict": "WRONG_ANSWER",
                    "runtimeMs": runtime_ms,
                    "memoryKb": None,
                    "message": "Output did not match the expected value",
                    "actualOutput": actual_output,
                }
            )
            case_results.extend(build_not_run_cases(test_cases, index + 1))
            overall_verdict = "WRONG_ANSWER"
            break

    write_text(STDOUT_FILE, "\n".join(value for value in combined_stdout if value))
    write_text(STDERR_FILE, "\n".join(value for value in combined_stderr if value))
    write_result(
        {
            "verdict": overall_verdict,
            "runtimeMs": total_runtime_ms if case_results else None,
            "memoryKb": None,
            "executionLog": f"{language} sandbox completed with verdict {overall_verdict}",
            "compilerOutput": (compile_stdout + "\n" + compile_stderr).strip() or None,
            "stdout": "\n".join(value for value in combined_stdout if value),
            "stderr": "\n".join(value for value in combined_stderr if value),
            "testCaseResults": case_results,
        }
    )
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as exc:  # pragma: no cover - last-resort sandbox fallback
        write_result(
            {
                "verdict": "INTERNAL_ERROR",
                "runtimeMs": None,
                "memoryKb": None,
                "executionLog": "Sandbox runner crashed unexpectedly",
                "compilerOutput": None,
                "stdout": "",
                "stderr": str(exc),
                "testCaseResults": [],
            }
        )
        sys.exit(1)