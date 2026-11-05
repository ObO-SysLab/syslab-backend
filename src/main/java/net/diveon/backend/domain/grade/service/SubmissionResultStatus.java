package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.contest.entity.ContestSubmission;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveSubmission;

/** Read-only verdict projection. Lifecycle failure is not a judged code error. */
public final class SubmissionResultStatus {
    private SubmissionResultStatus() {}

    public static String from(SolveSubmission submission, SolveResult result) {
        if (submission == null || submission.getSubmissionState() != SolveSubmission.SubmissionState.COMPLETED
                || result == null || result.getResultState() == null) return null;
        return result.getResultState().name();
    }

    public static String fromContest(ContestSubmission submission, SolveResult result) {
        if (!"COMPLETED".equals(submission.getSubmissionStatus())) return null;
        if (submission.getSolveSubmission() != null) return from(submission.getSolveSubmission(), result);
        // Immediate objective/practice submissions store their verdict on the contest row itself.
        String type = submission.getContestProblem().getProblem().getType();
        if (!("objective".equals(type) || "practice".equals(type)) || submission.getIsCorrect() == null) return null;
        return submission.getIsCorrect() ? "CORRECT" : "WRONG";
    }
}
