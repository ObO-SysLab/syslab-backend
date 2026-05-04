package net.diveon.backend.domain.grade.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionObjective;
import net.diveon.backend.domain.grade.entity.SolveSubmissionPractice;
import net.diveon.backend.domain.grade.entity.SolveResult;
import net.diveon.backend.domain.grade.entity.SolveSubmission.SubmissionState;
import net.diveon.backend.domain.grade.entity.SolveResult.SolveResultState;
import net.diveon.backend.domain.grade.repository.SolveResultRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionObjectiveRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionPracticeRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
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
    private final CodingGradeQueueService codingGradeQueueService;

    public SubmissionGradeAsyncService(UserRepository userRepository,
        ProblemRepository problemRepository,
        ProblemObjectiveRepository problemObjectiveRepository, 
        ProblemCodingRepository problemCodingRepository, 
        ProblemPracticeRepository problemPracticeRepository,
        SolveSubmissionRepository solveSubmissionRepository, 
        SolveSubmissionObjectiveRepository solveSubmissionObjectiveRepository,
        SolveSubmissionCodingRepository solveSubmissionCodingRepository,
        SolveSubmissionPracticeRepository solveSubmissionPracticeRepository,
        SolveResultRepository solveResultRepository,
        CodingGradeQueueService codingGradeQueueService
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
        this.codingGradeQueueService = codingGradeQueueService;
    }



    //추가로 디자인 패턴 적용해서, 이거는 수정하는게 좋을 듯합니다.
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
        System.out.println("객관식 문제 채점시작>>>>" + probId + "<<문제번호" + submissionId +"<<제출번호" + submitterId + "<<제출자 번호");
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


        submission.setSubmissionState(SubmissionState.JUDGING);
        //Seqeunce 1 -  data check - end

        //Seqeunce 2 -  grading - start
        List<Integer> correctAnswer = problemObjective.getAnswer();
        List<Integer> submittedAnswer = submissionObjective.getAnswer();

        boolean correctness = correctAnswer.equals(submittedAnswer);

        if(correctness){
            //정답
            SolveResult result = new SolveResult(submission, SolveResultState.CORRECT);
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }else{
            //오답
            SolveResult result = new SolveResult(submission, SolveResultState.WRONG);
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }
    }
    public void gradeProblemPractice(long probId, 
        long submitterId, 
        long submissionId
    ){
        System.out.println("실습형 문제 채점 시작>>>>" + probId + "<<문제번호" + submissionId +"<<제출번호" + submitterId + "<<제출자 번호");
        //Seqeunce 1 -  data check - start
        //grade objective problems
        //TODO: must be improved
        User submitter = userRepository.findById(submitterId).orElseThrow();
        
        //TODO: must be improved
        SolveSubmission submission = solveSubmissionRepository.findById(submissionId)
        .orElseThrow();

        //TODO: must be improved
        SolveSubmissionPractice solveSubmissionPractice = solveSubmissionPracticeRepository.findById(submissionId)
        .orElseThrow();

        //TODO: must be improved
        Problem problem = problemRepository.findById(probId)
        .orElseThrow();

        //TODO: must be improved
        ProblemPractice problemPractice = problemPracticeRepository.findById(probId)
        .orElseThrow();


        submission.setSubmissionState(SubmissionState.JUDGING);
        //Seqeunce 1 -  data check - end

        //Seqeunce 2 -  grading - start
        String correctAnswer = problemPractice.getFlagHash();
        String userAnswer = hashFlag(solveSubmissionPractice.getAnswer());
        boolean correctness = correctAnswer.equals(userAnswer);

        if(correctness){
            //정답
            SolveResult result = new SolveResult(submission, SolveResultState.CORRECT);
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }else{
            //오답
            SolveResult result = new SolveResult(submission, SolveResultState.WRONG);
            solveResultRepository.save(result);
            submission.setSubmissionState(SubmissionState.COMPLETED);
        }    
    }

    public void gradeProblemCoding(long probId, 
        long submitterId, 
        long submissionId
    ){
        codingGradeQueueService.sendGradeRequest(probId, submitterId, submissionId);
    }

    private String hashFlag(String flag) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(flag.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    

}
