package net.diveon.backend.domain.problem.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import net.diveon.backend.domain.problem.dto.request.ProblemCreateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCreateObjectiveResponse;
import net.diveon.backend.domain.problem.service.ProblemCreateService;
import net.diveon.backend.global.response.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import net.diveon.backend.global.response.ApiResponse;

@RestController
@RequestMapping("/api/problems")
public class ProblemCreateController {
    
    private final ProblemCreateService problemCreateService;

    public ProblemCreateController(ProblemCreateService problemCreateService){
        this.problemCreateService = problemCreateService;
    }

    /**
     * 
     * @param request //dto
     * @param userId //jwt-AuthenticationPrincipal
     * @return ResponseEntity<ApiResPonse<body> //
     */
    @PostMapping("/objective")
    public ResponseEntity<ApiResponse<ProblemCreateObjectiveResponse>> createObjectiveProblem(@Valid @RequestBody ProblemCreateObjectiveRequest request, 
        @AuthenticationPrincipal String userId) {
        //TODO: process POST request

        //아래는 임시 비즈니스 로직으로 변경해야함
        ProblemCreateObjectiveResponse responseBody = new ProblemCreateObjectiveResponse();
        
        //실제 비즈니스 로직, 서비스 호출
        responseBody = problemCreateService.createObjective(request, userId);

        // 이거 호출함수 형태 수정해야함, 우리가 200 말고 다른 값을 넣을수없음(ApiResopone.success())
        return ResponseEntity.status(201).body(ApiResponse.success("문제가 등록되었습니다.", responseBody));
    }
    
}
