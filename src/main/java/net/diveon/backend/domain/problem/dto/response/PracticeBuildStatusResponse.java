package net.diveon.backend.domain.problem.dto.response;

import net.diveon.backend.domain.problem.entity.ProblemPractice;

/** Deliberately excludes infrastructure identifiers, artifact locations, and raw failure details. */
public record PracticeBuildStatusResponse(String status, boolean canStart, String message) {
    public static PracticeBuildStatusResponse of(ProblemPractice practice, boolean canExecute) {
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
                    : "실습 환경을 제작하지 못했습니다. 문제 번호와 함께 운영자에게 문의해 주세요.";
            return new PracticeBuildStatusResponse("FAILED", false, message);
        }
        return new PracticeBuildStatusResponse("UNAVAILABLE", false,
                "실습 환경 준비 상태를 확인할 수 없습니다. 문제 번호와 함께 운영자에게 문의해 주세요.");
    }
}
