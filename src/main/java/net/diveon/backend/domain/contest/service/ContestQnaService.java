package net.diveon.backend.domain.contest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.QnaAnswerRequest;
import net.diveon.backend.domain.contest.dto.request.QnaCreateRequest;
import net.diveon.backend.domain.contest.dto.response.QnaListResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestQna;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestQnaRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.exception.ContestQnaNotFoundException;

@Service
public class ContestQnaService {

    private final ContestRepository contestRepository;
    private final ContestQnaRepository contestQnaRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final UserRepository userRepository;

    public ContestQnaService(ContestRepository contestRepository,
                             ContestQnaRepository contestQnaRepository,
                             ContestParticipantRepository contestParticipantRepository,
                             UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.contestQnaRepository = contestQnaRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.userRepository = userRepository;
    }

    // Q&A 목록 조회 (참가자만)
    @Transactional(readOnly = true)
    public QnaListResponse getQnaList(Long contestId, Long userId) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        List<ContestQna> qnas = contestQnaRepository.findAllByContestIdOrderByCreatedAtDesc(contestId);
        List<QnaListResponse.QnaItem> items = qnas.stream().map(QnaListResponse.QnaItem::of).toList();
        return new QnaListResponse(items);
    }

    // 질문 생성 (참가자)
    @Transactional
    public void createQuestion(Long contestId, Long userId, QnaCreateRequest request) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        contestQnaRepository.save(new ContestQna(contest, user, request.getQuestion()));
    }

    // 질문 삭제 (관리자 또는 작성자)
    @Transactional
    public void deleteQuestion(Long contestId, Long qnaId, Long userId) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        ContestQna qna = contestQnaRepository.findByIdAndContestId(qnaId, contestId)
                .orElseThrow(ContestQnaNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        boolean isAdmin = participant.getRole() == ContestParticipant.ContestRole.ADMIN;
        boolean isAuthor = qna.getQuestioner().getId().equals(userId);

        if (!isAdmin && !isAuthor) {
            throw new ContestAccessDeniedException();
        }

        contestQnaRepository.delete(qna);
    }

    // 답변 생성 (관리자)
    @Transactional
    public void createAnswer(Long contestId, Long qnaId, Long userId, QnaAnswerRequest request) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        ContestQna qna = contestQnaRepository.findByIdAndContestId(qnaId, contestId)
                .orElseThrow(ContestQnaNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() != ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        qna.answer(user, request.getAnswer());
    }

    // 답변 삭제 (관리자)
    @Transactional
    public void deleteAnswer(Long contestId, Long qnaId, Long userId) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        ContestQna qna = contestQnaRepository.findByIdAndContestId(qnaId, contestId)
                .orElseThrow(ContestQnaNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() != ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        qna.answer(null, null);
    }
}
