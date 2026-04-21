package net.diveon.backend.domain.problem.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemUpdateObjectiveResponse;
import net.diveon.backend.domain.problem.service.ProblemUpdateService;
import net.diveon.backend.global.response.ApiResponse;


@RestController
@RequestMapping("/api/problems")
public class ProblemUpdateController {
    private final ProblemUpdateService problemUpdateService;

    public ProblemUpdateController(ProblemUpdateService problemUpdateService){
        this.problemUpdateService = problemUpdateService;
    }


    @PatchMapping("/{prob_id}")
    public ResponseEntity<ApiResponse<ProblemUpdateObjectiveResponse>> updateProblemObjective(@AuthenticationPrincipal long userId, 
        @RequestBody ProblemUpdateObjectiveRequest request,
        @PathVariable long probId){


            ProblemUpdateObjectiveResponse responseData = new ProblemUpdateObjectiveResponse();
            
            responseData = problemUpdateService.updateProblemObjective(String.valueOf(userId), probId, request);
            // 요아래 대충이라 수정필요함 2024.04.21  - 안상완
            //TODO 실제로 응답 body의 data 부분 객체 할당, 이후 응답 제대로 만들기
            
            return ResponseEntity.status(200).body(ApiResponse.success("문제 변경에 성공했습니다.", responseData));
    }
}
