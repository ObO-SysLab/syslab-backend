package net.diveon.backend.domain.problem.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.diveon.backend.domain.problem.entity.ProblemPractice;

/**
 * Deliberately excludes infrastructure identifiers, artifact locations, and raw failure details.
 * failure carries only the validated reason code (and command name) of a failed build, for its author.
 */
public record PracticeBuildStatusResponse(String status, boolean canStart, String message,
        @JsonInclude(JsonInclude.Include.NON_NULL) Failure failure) {
    public record Failure(String code, @JsonInclude(JsonInclude.Include.NON_NULL) String command) {}

    public PracticeBuildStatusResponse(String status, boolean canStart, String message) {
        this(status, canStart, message, null);
    }

    public static PracticeBuildStatusResponse of(ProblemPractice practice, boolean canExecute) {
        return of(practice, canExecute, false);
    }

    public static PracticeBuildStatusResponse of(ProblemPractice practice, boolean canExecute, boolean isAuthor) {
        String status = practice.getImageStatus();
        if ("READY".equals(status)) {
            // Legacy READY prob-* images remain supported by the VM service.
            return new PracticeBuildStatusResponse("READY", canExecute, canExecute
                    ? "실습 환경이 준비되었습니다. 실습을 시작할 수 있습니다."
                    : "실습 환경은 준비되었지만 현재 계정으로 실행할 수 없습니다.");
        }
        if ("PENDING".equals(status)) {
            return new PracticeBuildStatusResponse("PENDING", false,
                    "실습 환경을 제작하고 있습니다. 준비가 끝나면 실습을 시작할 수 있습니다.");
        }
        if ("FAILED".equals(status)) {
            String reason = practice.getBuildFailureReason();
            String message = "TIMED_OUT".equals(reason) || "DEADLINE_EXCEEDED".equals(reason)
                    ? "실습 환경 제작 제한 시간을 초과했습니다. 문제 번호와 함께 운영자에게 문의해 주세요."
                    : practice.getCommandAllowlist() != null
                    ? "실습 환경을 제작하지 못했습니다. 허용 명령어가 모두 이미지에 설치돼 있는지 확인해 주세요."
                    : "실습 환경을 제작하지 못했습니다. 문제 번호와 함께 운영자에게 문의해 주세요.";
            return new PracticeBuildStatusResponse("FAILED", false, message,
                    isAuthor ? failure(practice.getBuildFailureDetail()) : null);
        }
        return new PracticeBuildStatusResponse("UNAVAILABLE", false,
                "실습 환경 준비 상태를 확인할 수 없습니다. 문제 번호와 함께 운영자에게 문의해 주세요.");
    }

    private static Failure failure(String detail) {
        if (detail == null || detail.isBlank()) return null;
        int split = detail.indexOf(':');
        return split < 0 ? new Failure(detail, null) : new Failure(detail.substring(0, split), detail.substring(split + 1));
    }
}
