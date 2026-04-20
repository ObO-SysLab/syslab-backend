package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Service;

import net.diveon.backend.domain.problem.dto.response.ProblemDeleteResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;


/**
 * <pre>
 * 해당 서비스는 문제를 제거하는 서비스
 * 입력은 /api/problems/{prob_id} 와 같은식으로 path parameter로써 부여
 * 사용자 인증은 JWT 토큰을 이용하여 id를 추출
 * Endpoint `/api/problems/{prob_id}`
 *  Method: `DELETE`
 *  Description 본인이 생성한 문제를 삭제한다. 현재 진행 중인 대회에 속한 문제는 삭제할 수 없다.
 *  Authentication `Bearer Token`
 * 
 * @Param endpoint parameter
 * </pre>
 */

@Service
public class ProblemDeleteService {
    // 일단은 객관식 문제에 대한 제거만 구현
    /**
     * 객관식: 구현중
     * 코드형: 놉
     * 실습형: 놉
     */

    //

    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;

    
    public ProblemDeleteService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository){
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
    }


    public ProblemDeleteResponse deleteProblemObjective(String userId, Long prodId){
        Problem problem = problemRepository.findById(prodId).orElseThrow();
        ProblemObjective problemObjective = problemObjectiveRepository.findById(prodId).orElseThrow();
        
        problemObjectiveRepository.deleteById(prodId);
        problemRepository.deleteById(prodId);
        
        return new ProblemDeleteResponse(prodId);
    }
}
