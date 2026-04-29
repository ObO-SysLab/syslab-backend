package net.diveon.backend.domain.problem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.problem.dto.response.ProblemDeleteResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.OboStepRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;


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
    /**
     * 객관식: 구현중
     * 코드형: 놉
     * 실습형: 구현중
     */

    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final OboStepRepository oboStepRepository;

    public ProblemDeleteService(ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository,
        ProblemCodingRepository problemCodingRepository,
        OboStepRepository oboStepRepository){
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.oboStepRepository = oboStepRepository;
    }

    // 분기 로직
    @Transactional
    public ProblemDeleteResponse deleteProblem(long userId, long probId){
        Problem problem = problemRepository.findById(probId)
            .orElseThrow(() -> new ProblemNotFoundException(probId + "번에 해당하는 문제가 존재하지 않습니다."));

        // TODO: author 권한 체크 추가 예정 (추후에 problem.getAuthor()로 접근 가능하도록 Problem 객체로 받음)

        if (problem.getType().equals("practice")) {
            return deleteProblemPractice(userId, probId);
        } else if (problem.getType().equals("objective")) {
            return deleteProblemObjective(userId, probId);
        } else if (problem.getType().equals("coding")) {
            return deleteProblemCoding(userId, probId);
        } else {
            throw new ProblemNotFoundException(probId + "번 문제의 타입을 알 수 없습니다.");
        }
    }

    // 객관식형
    @Transactional
    public ProblemDeleteResponse deleteProblemObjective(long userId, long prodId){

        // // 아래 코드는 검증을 좀더 깔끔한 방식으로 할 수 있다면 좋을듯.
        // problemRepository.findById(prodId).orElseThrow(ProblemNotFoundException::new);
        // problemObjectiveRepository.findById(prodId).orElseThrow(ProblemNotFoundException::new);


        if(!problemRepository.existsById(prodId)){
            throw new ProblemNotFoundException("문제 전체 중" + prodId +"번에 해당하는 문제 자체가 존재하지 않습니다.");
        }
        if(!problemObjectiveRepository.existsById(prodId)){
            throw new ProblemNotFoundException("객관식 문제 중" + prodId +"번에 해당하는 문제 자체가 존재하지 않습니다.");
        }

        //아래는 다른 방식으로, 예외시 기본 생성자 말고, 다른 생성자를 사용할 수 있는 방법.
        // Problem problem = problemRepository.findById(prodId)
        //     .orElseThrow(() -> new ProblemNotFoundException("ID " + prodId + "번에 해당하는 문제를 찾을 수 없습니다."));

        // 널값에 대항할 수 있는 로직이 필요하다. 만약 long 이 아니라 Long 을 쓴다면.
        // Problem problem = problemRepository.findById(prodId).orElseThrow();
        // List<OboStep> oboSteps = oboStepRepository.findByProblemOrderByStepAsc(problem);
        // if (!oboSteps.isEmpty()) {
        //     oboStepRepository.deleteAll(oboSteps);
        // }
        oboStepRepository.deleteByProblem_Id(prodId);
        problemObjectiveRepository.deleteById(prodId);
        problemRepository.deleteById(prodId);

        return new ProblemDeleteResponse(prodId);
    }

    // 실습형
    @Transactional
    public ProblemDeleteResponse deleteProblemPractice(long userId, long probId){
        problemPracticeRepository.deleteById(probId);
        problemRepository.deleteById(probId);

        return new ProblemDeleteResponse(probId);
    }

    // 코딩형
    @Transactional
    public ProblemDeleteResponse deleteProblemCoding(long userId, long probId){
        problemCodingRepository.deleteById(probId);
        problemRepository.deleteById(probId);

        return new ProblemDeleteResponse(probId);
    }
}