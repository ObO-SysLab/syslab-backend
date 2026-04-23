package net.diveon.backend.domain.problem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdatePracticeRequest;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdateCodingRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdatePracticeResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateCodingResponse;
import net.diveon.backend.domain.problem.service.ProblemUpdateService;
import net.diveon.backend.global.response.ApiResponse;


@RestController
@RequestMapping("/api/problems")
public class ProblemUpdateController {
    private final ProblemUpdateService problemUpdateService;

    public ProblemUpdateController(ProblemUpdateService problemUpdateService){
        this.problemUpdateService = problemUpdateService;
    }

    // 객관식
    @PatchMapping("/{prob_id}")
    public ResponseEntity<ApiResponse<ProblemUpdateObjectiveResponse>> updateProblemObjective(@AuthenticationPrincipal String userId, 
        @RequestBody ProblemUpdateObjectiveRequest request,
        @PathVariable("prob_id") long probId){


            ProblemUpdateObjectiveResponse responseData = new ProblemUpdateObjectiveResponse();
            
            responseData = problemUpdateService.updateProblemObjective(Long.parseLong(userId), probId, request);
            // 요아래 대충이라 수정필요함 2024.04.21  - 안상완
            //TODO 실제로 응답 body의 data 부분 객체 할당, 이후 응답 제대로 만들기
            
            return ResponseEntity.status(200).body(ApiResponse.success("문제 변경에 성공했습니다.", responseData));
    }
    
    // 실습형
    @PatchMapping("/practice/{prob_id}")
    public ResponseEntity<ApiResponse<ProblemUpdatePracticeResponse>> updateProblemPractice(
            @AuthenticationPrincipal String userId,
            @RequestBody ProblemUpdatePracticeRequest request,
            @PathVariable("prob_id") long probId) {

        ProblemUpdatePracticeResponse responseData = problemUpdateService.updateProblemPractice(userId, probId, request);
        return ResponseEntity.status(200).body(ApiResponse.success("문제가 수정되었습니다.", responseData));
    }

    // 코딩형
    @PatchMapping("/coding/{prob_id}")
    public ResponseEntity<ApiResponse<ProblemUpdateCodingResponse>> updateProblemCoding(
            @AuthenticationPrincipal String userId,
            @RequestBody ProblemUpdateCodingRequest request,
            @PathVariable("prob_id") long probId) {

        ProblemUpdateCodingResponse responseData = problemUpdateService.updateProblemCoding(userId, probId, request);
        return ResponseEntity.status(200).body(ApiResponse.success("문제가 수정되었습니다.", responseData));
    }
}
