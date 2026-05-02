package net.diveon.backend.domain.grade.service;

import java.util.List;

import org.springframework.stereotype.Service;

import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionObjective;
import net.diveon.backend.domain.grade.entity.SovleResult;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SovleResult.SovleResultState;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionObjectiveRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionPracticeRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.repository.ProblemCodingRepository;
import net.diveon.backend.domain.problem.repository.ProblemObjectiveRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubmissionGradeAsyncService {
    //서비스 필요 데이터 정보
    /**
     * <pre>
     * ## 레포지토리
     * - 유저
     * 
     * - 문제
     * - 문제3종
     * 
     * - submission
     * - submission 3종
     * 
     * - result
     * - result 3종(아마도)
     * ## 객체
     * - 레포와 동일
     * </pre>
     */

    private final UserRepository userRepository;

    private final ProblemRepository problemRepository;
    private final ProblemObjectiveRepository problemObjectiveRepository;
    private final ProblemCodingRepository problemCodingRepository; 
    private final ProblemPracticeRepository problemPracticeRepository;

    private final SolveSubmissionRepository solveSubmissionRepository;
    private final SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository;
    private final SolveSubmissionCodingRepository solveSubmissionCodingRepository;
    private final SolveSubmissionPracticeRepository solveSubmissionPracticeRepository;

    private final SolveResultRepository solveResultRepository;

    public SubmissionGradeAsyncService(UserRepository userRepository,
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository, 
        ProblemCodingRepository problemCodingRepository, 
        ProblemPracticeRepository problemPracticeRepository,
        SolveSubmissionRepository solveSubmissionRepository, 
        SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveSubmissionPracticeRepository solveSubmissionPracticeRepository,
        SolveResultRepository solveResultRepository
    ){
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.problemObjectiveRepository = problemObjectiveRepository;
        this.problemCodingRepository = problemCodingRepository;
        this.problemPracticeRepository = problemPracticeRepository;

        this.solveResultRepository = solveResultRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
        this.solveSubmissionCodingRepository = solveSubmissionCodingRepository;
        this.solveSubmissionPracticeRepository = solveSubmissionPracticeRepository;
        this.solveSubmissionObjectiveRepository = solveSubmissionObjectiveRepository;
    }


    /**
     * <pre>
     * 채점로직은 다음과 같이 동작함
     * 1. 정보 확인
     * 2. 정보 저장
     * 2. 채점 진행
     * 3. 정보 저장
     * 4. 채점 완료
     * 5. 정보 저장
     * </pre>
     * @param probId
     * @param submitterId
     * @param submissionId
     */
    public void gradeProblemObjective(long probId, 
        long submitterId, 
        long submissionId
    ){
        System.out.println("출력시작>>>>" + probId + "<<문제번호" + submissionId +"<<제출번호" + submitterId + "<<제출자 번호");
        //Seqeunce 1 -  data check - start
        //grade objective problems
        //TODO: must be improved
        User submitter = userRepository.findById(submitterId).orElseThrow();
        
        //TODO: must be improved
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
        .orElseThrow();

        //TODO: must be improved
        SolveSubmissionObjective submissionObjective = solveSubmissionObjectiveRepository.findById(submissionId)
        .orElseThrow();

        //TODO: must be improved
        Problem problem = problemRepository.findById(probId)
        .orElseThrow();

        //TODO: must be improved
        ProblemObjective problemObjective = problemObjectiveRepository.findById(probId)
        .orElseThrow();


        submission.setSubmissionState(SubmissionState.INPROGRESS);
        //Seqeunce 1 -  data check - end

        //Seqeunce 2 -  grading - start
        List<Integer> correctAnswer = problemObjective.getAnswer();
        List<Integer> submittedAnswer = submissionObjective.getAnswer();

        boolean correctness = correctAnswer.equals(submittedAnswer);

        if(correctness){
            //정답
            SovleResult result = new SovleResult(submission, SovleResultState.CORRECT, "정답입니다.");
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }else{
            //오답
            SovleResult result = new SovleResult(submission, SovleResultState.WRONG, "오답입니다..");
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }




        //Seqeunce 2 -  grading - end


        //Seqeunce 3 -  data check - end
        //Seqeunce 3 -  data check - end
        
        //Seqeunce 4 -  data check - end
        //Seqeunce 4 -  data check - end

        //Seqeunce 5 -  data check - end
        //Seqeunce 5 -  data check - end

        //Seqeunce 6 -  data check - end
        //Seqeunce 6 -  data check - end  
    }
    public void gradeProblemPractice(long probId, 
        long submitterId, 
        long submissionId
    ){
        
    }

    public void gradeProblemCoding(long probId, 
        long submitterId, 
        long submissionId
    ){
        
    }

    

}
