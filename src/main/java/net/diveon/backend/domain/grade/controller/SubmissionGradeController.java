package net.diveon.backend.domain.grade.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.grade.dto.response.SubmissionGradeResponse;
import net.diveon.backend.domain.grade.service.SubmissionGradeAsyncService;
import net.diveon.backend.domain.grade.service.SubmissionGradeStarterService;
import net.diveon.backend.global.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/submissions")
public class SubmissionGradeController {
    private final SubmissionGradeAsyncService submissionGradeAsyncService;
    private final SubmissionGradeStarterService submissionGradeStarterService;

    public SubmissionGradeController(
        SubmissionGradeAsyncService submissionGradeAsyncService,
        SubmissionGradeStarterService submissionGradeStarterService
    ){
        this.submissionGradeAsyncService = submissionGradeAsyncService;
        this.submissionGradeStarterService = submissionGradeStarterService;
    }


    @PostMapping("/grade/{probId}")
    public ResponseEntity<ApiResponse<SubmissionGradeResponse>> grade(@AuthenticationPrincipal String userId,
        @Valid @RequestBody SubmissionGradeReqeust reqeust
    ){
        SubmissionGradeResponse initResponse = submissionGradeStarterService.submissionGradeStart(Long.parseLong(userId), reqeust);
        long submissionId = initResponse.getSubmissionId();

        //long probId, 
        // long submitterId, 
        // long submissionId
        submissionGradeAsyncService.gradeProblemObjective(reqeust.getProdId(), Long.parseLong(userId), submissionId);
        

        return ResponseEntity.status(200).body(ApiResponse.success("접수되었습니다.", initResponse));
    }
    
}
