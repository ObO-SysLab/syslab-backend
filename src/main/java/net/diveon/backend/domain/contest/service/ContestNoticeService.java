package net.diveon.backend.domain.contest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.NoticeCreateRequest;
import net.diveon.backend.domain.contest.dto.request.NoticeUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.NoticeListResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestNotice;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.repository.ContestNoticeRepository;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestNoticeNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class ContestNoticeService {

    private final ContestRepository contestRepository;
    private final ContestNoticeRepository contestNoticeRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final UserRepository userRepository;

    public ContestNoticeService(ContestRepository contestRepository,
                                ContestNoticeRepository contestNoticeRepository,
                                ContestParticipantRepository contestParticipantRepository,
                                UserRepository userRepository) {
        this.contestRepository = contestRepository;
        this.contestNoticeRepository = contestNoticeRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NoticeListResponse getNoticeList(Long contestId) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        List<ContestNotice> notices = contestNoticeRepository.findAllByContestIdOrderByCreatedAtDesc(contestId);
        List<NoticeListResponse.NoticeItem> items = notices.stream().map(NoticeListResponse.NoticeItem::of).toList();
        return new NoticeListResponse(items);
    }

    @Transactional
    public void createNotice(Long contestId, Long userId, NoticeCreateRequest request) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() != ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        contestNoticeRepository.save(new ContestNotice(contest, user, request.getTitle(), request.getContent()));
    }

    @Transactional
    public void updateNotice(Long contestId, Long noticeId, Long userId, NoticeUpdateRequest request) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        ContestNotice notice = contestNoticeRepository.findByIdAndContestId(noticeId, contestId)
                .orElseThrow(ContestNoticeNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() != ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        notice.update(request.getTitle(), request.getContent());
    }

    @Transactional
    public void deleteNotice(Long contestId, Long noticeId, Long userId) {
        contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        ContestNotice notice = contestNoticeRepository.findByIdAndContestId(noticeId, contestId)
                .orElseThrow(ContestNoticeNotFoundException::new);
        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() != ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        contestNoticeRepository.delete(notice);
    }
}
