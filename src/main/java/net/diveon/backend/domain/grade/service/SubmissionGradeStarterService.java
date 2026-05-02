package net.diveon.backend.domain.grade.service;

import java.util.List;

import org.springframework.stereotype.Service;
//반드시 요 아래놈으로 써야함, 그냥 자카르타로쓰면 별로임
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.grade.dto.request.SubmissionGradeReqeust;
import net.diveon.backend.domain.grade.dto.response.SubmissionGradeResponse;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.entity.SolveSubmissionObjective;
import net.diveon.backend.domain.grade.entity.SolveSubmissionPractice;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionObjectiveRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionPracticeRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;

@Service
public class SubmissionGradeStarterService {
    private final SolveResultRepository solveResultRepository; 
    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveSubmissionPracticeRepository solveSubmissionPracticeRepository;


    private final UserRepository userRepository;

    private final ProblemRepository problemRepository;
    private final ProblemCodingRepository problemCodingRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemPracticeRepository problemPracticeRepository;



    public SubmissionGradeStarterService(SolveResultRepository solveResultRepository, 
        SolveSubmissionRepository solveSubmissionRepository,
        SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveSubmissionPracticeRepository solveSubmissionPracticeRepository,
        UserRepository userRepository,
        ProblemRepository problemRepository,
        ProblemCodingRepository problemCodingRepository,
        ProblemObjectiveRepository problemObjectiveRepository,
        ProblemPracticeRepository problemPracticeRepository
    ){
        this.solveResultRepository = solveResultRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionObjectiveRepository = solveSubmissionObjectiveRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveSubmissionPracticeRepository = solveSubmissionPracticeRepository;

        this.userRepository = userRepository;

        this.problemRepository = problemRepository;
        this.problemCodingRepository =problemCodingRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemPracticeRepository = problemPracticeRepository;
    }

    @Transactional
    public SubmissionGradeResponse submissionGradeStart(long userId,
        SubmissionGradeReqeust request // probId, answer, language 존재
    ){
        /**
         * 함수 내부 구현 흐름
         * 1. 각종 확인(사용자 확인 등)
         * 2. submission 생성
         * 3. DB에 저장
         * 4. 비동기로 채점 로직 호출
         * 5. 순서상 4는 비동기이므로 곧바로 응답 반환(풀링용)
         */
        
        //sequence 1- check data - start
        
        //TODO: exception logic must be improved
        User submittUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("요청한 사용자는 없는 사용자입니다."));

        //@valid로 검증된 상태일 것임 but
        //TODO: must be improved.
        long probId = request.getProdId();
        
        //TODO: exception logic must be improved
        Problem problem = problemRepository.findById(probId).orElseThrow(() -> new RuntimeException("문제가 없습니다."));

        // objective / coding / practice
        String problemType = problem.getType();

        //new submission create
        //Create State - SubmissionState.PENDING, LocalDateTime.now()
        SolveSubmission submission = new SolveSubmission(problem, submittUser);

        //save submission
        solveSubmissionRepository.save(submission);

        //call asnyc function for each problem type
        //TODO: Must COMPLETE FUCNTION CALL LOGIC
        if(problemType.equals("objective")){
            // SolveSubmissionObjective submissionObjective = new SolveSubmissionObjective(reqeust.getAnswer())
            //TODO: dto 내부에서 변환하는것으로 바꾸기
            if (request.getAnswer() instanceof List) {
                // 안전하게 형변환 후 전달
                List<Integer> answerList = (List<Integer>) request.getAnswer();
                SolveSubmissionObjective submissionObjective = new SolveSubmissionObjective(submission, answerList);
                solveSubmissionObjectiveRepository.save(submissionObjective);
                
            }else{
                //TODO : exception must be improved
                throw new RuntimeException("객관식 정답 리스트 형식 변환에 문제가 있습니다.");
            }
        }else if(problemType.equals("coding")){

        }else if(problemType.equals("practice")){
            if (request.getAnswer() instanceof String) {
                String answer = (String) request.getAnswer();
                SolveSubmissionPractice submissionPractice = new SolveSubmissionPractice(submission, answer);
                solveSubmissionPracticeRepository.save(submissionPractice);
            }else{
                //TODO : exception must be improved
                throw new RuntimeException("실습형 정답 문자열 형식 변환에 문제가 있습니다.");
            }
        }else{
            //eception
            //TODO: must be improved.
            throw new RuntimeException("문제 유형이 3가지중 어느것도 아닙니다.");
        }
/**
 * // 2. 트랜잭션 커밋 성공 시에만 비동기 로직 실행 예약
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            asyncService.gradeProblemObjective(submission.getId(), ...);
        }
    });
 */
        //sequence 1- check data - end
        
        //sequence 1- check data - start
        //sequence 1- check data - start
        //sequence 1- check data - start
        //sequence 1- check data - start
        //sequence 1- check data - start




        // 그냥 오류 방지용, 바꿔야함
        return new SubmissionGradeResponse(submission.getId(), probId, SubmissionState.PENDING.name());
    }
}
