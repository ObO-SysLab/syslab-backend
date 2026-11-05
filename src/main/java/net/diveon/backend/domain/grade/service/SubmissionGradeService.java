package net.diveon.backend.domain.grade.service;

import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.grade.dto.response.SubmissionGradeResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SubmissionGradeService {
    private final SubmissionGradeStarterService starter;
    private final SubmissionGradeAsyncService grader;
    private final TransactionTemplate transaction;
    private final boolean recoveryEnabled;

    public SubmissionGradeService(SubmissionGradeStarterService starter, SubmissionGradeAsyncService grader,
            PlatformTransactionManager transactionManager,
            @Value("${grading.recovery.enabled:false}") boolean recoveryEnabled) {
        this.starter = starter;
        this.grader = grader;
        this.transaction = new TransactionTemplate(transactionManager);
        this.recoveryEnabled = recoveryEnabled;
    }

    // Submission, counter and durable coding request commit together, or all roll back.
    public SubmissionGradeResponse submit(long userId, SubmissionGradeReqeust request) {
        if (recoveryEnabled) return transaction.execute(status -> submitRequest(userId, request));
        // Preserve legacy commit ordering while the coordinated rollout is disabled.
        return submitRequest(userId, request);
    }

    private SubmissionGradeResponse submitRequest(long userId, SubmissionGradeReqeust request) {
        var response = starter.submissionGradeStart(userId, request);
        switch (response.getProblemType()) {
            case "objective" -> grader.gradeProblemObjective(request.getProdId(), userId, response.getSubmissionId());
            case "practice" -> grader.gradeProblemPractice(request.getProdId(), userId, response.getSubmissionId());
            case "coding" -> grader.gradeProblemCoding(request.getProdId(), userId, response.getSubmissionId());
            default -> throw new IllegalArgumentException("Unknown problem type");
        }
        return response;
    }
}
