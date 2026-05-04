package net.diveon.backend.domain.grade.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.grade.dto.response.SubmissionGradeResponse;
import net.diveon.backend.domain.grade.dto.response.SubmissionStatusResponse;
import net.diveon.backend.domain.grade.service.SubmissionGradeAsyncService;
import net.diveon.backend.domain.grade.service.SubmissionGradeStarterService;
import net.diveon.backend.domain.grade.service.SubmissionStatusService;
import net.diveon.backend.global.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/submissions")
public class SubmissionGradeController {
    private final SubmissionGradeAsyncService submissionGradeAsyncService;
    private final SubmissionGradeStarterService submissionGradeStarterService;
    private final SubmissionStatusService submissionStatusService;

    public SubmissionGradeController(
        SubmissionGradeAsyncService submissionGradeAsyncService,
        SubmissionGradeStarterService submissionGradeStarterService,
        SubmissionStatusService submissionStatusService
    ){
        this.submissionGradeAsyncService = submissionGradeAsyncService;
        this.submissionGradeStarterService = submissionGradeStarterService;
        this.submissionStatusService = submissionStatusService;
    }


    @PostMapping("/grade")
    public ResponseEntity<ApiResponse<SubmissionGradeResponse>> grade(@AuthenticationPrincipal String userId,
        @Valid @RequestBody SubmissionGradeReqeust reqeust
    ){
        SubmissionGradeResponse initResponse = submissionGradeStarterService.submissionGradeStart(Long.parseLong(userId), reqeust);
        long submissionId = initResponse.getSubmissionId();
        String problemType = initResponse.getProblemType();

        //long probId, 
        // long submitterId, 
        // long submissionId

        if(problemType.equals("objective")){
            submissionGradeAsyncService.gradeProblemObjective(reqeust.getProdId(), Long.parseLong(userId), submissionId);
        }else if(problemType.equals("practice")){
            submissionGradeAsyncService.gradeProblemPractice(reqeust.getProdId(), Long.parseLong(userId), submissionId);
        }else if(problemType.equals("coding")){
            submissionGradeAsyncService.gradeProblemCoding(reqeust.getProdId(), Long.parseLong(userId), submissionId);
        }else{
            throw new RuntimeException("문제 유형이 3가지중 어느것도 아닙니다.");
        }

        return ResponseEntity.status(200).body(ApiResponse.success("접수되었습니다.", initResponse));
    }

    @GetMapping("/{submissionId}/status")
    public ResponseEntity<ApiResponse<SubmissionStatusResponse>> getStatus(
        @AuthenticationPrincipal String userId,
        @PathVariable("submissionId") Long submissionId
    ) {
        SubmissionStatusResponse response = submissionStatusService.getStatus(Long.parseLong(userId), submissionId);
        return ResponseEntity.ok(ApiResponse.success("채점 상태 조회에 성공하였습니다.", response));
    }
    
}
