package net.diveon.backend.domain.grade.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import net.diveon.backend.domain.problem.entity.ProblemCoding;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;

/** Admission contract for grader/execution_protocol.py v1. Never rounds advertised limits. */
public final class CodingExecutionLimits {
    private static final int MIB = 1024 * 1024;
    private CodingExecutionLimits() {}

    public static int seconds(Integer milliseconds) {
        if (milliseconds == null || milliseconds < 1000 || milliseconds > 30000 || milliseconds % 1000 != 0) {
            fail("실행 시간은 1~30초 범위에서 정수 초로 설정해주세요.");
        }
        return milliseconds / 1000;
    }

    public static void problem(Integer timeMs, Integer memoryMb, List<String> languages, List<ForDtoTestCase> cases) {
        int seconds = seconds(timeMs);
        if (memoryMb == null || memoryMb < 16 || memoryMb > 1024) fail("메모리는 16~1024 MiB여야 합니다.");
        if (languages == null || languages.isEmpty() || languages.stream().anyMatch(l -> l == null || !List.of("python", "c", "cpp").contains(l))) {
            fail("지원하는 언어는 python, c, cpp입니다.");
        }
        if (cases == null || cases.isEmpty() || cases.size() > 100) fail("테스트는 1~100개여야 합니다.");
        if (20L + cases.size() * (seconds + 2L) > 600) fail("전체 실행 예산 600초를 초과합니다. 시간 제한이나 테스트 수를 줄여주세요.");
        // Reserve the maximum source plus JSON envelope; UTF-8 inputs become base64 in the runner bundle.
        long bundle = 4096L + encoded(MIB);
        for (ForDtoTestCase testcase : cases) {
            if (testcase == null) fail("테스트 입력과 정답이 필요합니다.");
            int input = bytes(testcase.getInput());
            int output = bytes(testcase.getOutput());
            if (input > MIB || output > MIB) fail("테스트별 입력·정답은 UTF-8 기준 각각 1 MiB 이하여야 합니다.");
            bundle += encoded(input) + 3;
        }
        if (bundle > 16L * MIB) fail("테스트 입력 묶음이 실행기 전송 한도 16 MiB를 초과합니다.");
    }

    public static void submission(ProblemCoding coding, String source) {
        problem(coding.getTimeLimitMs(), coding.getMemoryLimitMb(), coding.getAllowedLanguages(), coding.getTestcases());
        if (source == null || source.isBlank() || bytes(source) > MIB) fail("제출 코드는 UTF-8 기준 1 MiB 이하의 비어 있지 않은 문자열이어야 합니다.");
    }

    private static int bytes(String value) {
        if (value == null) fail("테스트 입력과 정답이 필요합니다. 빈 문자열은 허용됩니다.");
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
    private static long encoded(int bytes) { return 4L * ((bytes + 2L) / 3); }
    private static void fail(String message) { throw new InvalidSubmissionRequestException(message); }
}
