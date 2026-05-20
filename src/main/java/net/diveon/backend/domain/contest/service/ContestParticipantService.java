package net.diveon.backend.domain.contest.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.ContestParticipantBanRequest;
import net.diveon.backend.domain.contest.dto.response.ContestParticipantBanResponse;
import net.diveon.backend.domain.contest.dto.response.ContestParticipantListResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class ContestParticipantService {

    private final ContestRepository contestRepository;
    private final UserRepository userRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public ContestParticipantService(ContestRepository contestRepository,
                                     UserRepository userRepository,
                                     ContestParticipantRepository contestParticipantRepository) {
        this.contestRepository = contestRepository;
        this.userRepository = userRepository;
        this.contestParticipantRepository = contestParticipantRepository;
    }

    @Transactional(readOnly = true)
    public ContestParticipantListResponse getContestParticipants(Long contestId, Long userId, String keyword,
                                                                 int page, int size) {
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        ContestParticipant requester = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isContestCreator = contest.getCreatedBy().getId().equals(userId);
        boolean isContestAdmin = requester.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        String keywordFilter = keyword == null ? "" : keyword;
        Page<ContestParticipant> participantPage =
                contestParticipantRepository.findPageByContestIdWithUserOrderByJoinedAtAsc(
                        contestId, keywordFilter, PageRequest.of(page - 1, size));

        List<ContestParticipantListResponse.ParticipantItem> participants = participantPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new ContestParticipantListResponse(
                participantPage.getTotalElements(),
                participantPage.getTotalPages(),
                participants
        );
    }

    @Transactional(readOnly = true)
    public ContestParticipantListResponse getBannedContestParticipants(Long contestId, Long userId,
                                                                       String keyword, int page, int size) {
        userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        ContestParticipant requester = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isContestCreator = contest.getCreatedBy().getId().equals(userId);
        boolean isContestAdmin = requester.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        String keywordFilter = keyword == null ? "" : keyword;
        Page<ContestParticipant> participantPage =
                contestParticipantRepository.findPageByContestIdAndIsBannedTrueWithUserOrderByJoinedAtAsc(
                        contestId, keywordFilter, PageRequest.of(page - 1, size));

        List<ContestParticipantListResponse.ParticipantItem> participants = participantPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new ContestParticipantListResponse(
                participantPage.getTotalElements(),
                participantPage.getTotalPages(),
                participants
        );
    }

    @Transactional
    public ContestParticipantBanResponse updateContestParticipantBanStatus(
            Long contestId, Long participantUserId, Long requestUserId, ContestParticipantBanRequest request) {
        userRepository.findById(requestUserId).orElseThrow(UserNotFoundException::new);

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(ContestNotFoundException::new);

        ContestParticipant requester = contestParticipantRepository.findByContestIdAndUserId(contestId, requestUserId)
                .orElseThrow(ContestAccessDeniedException::new);

        boolean isContestCreator = contest.getCreatedBy().getId().equals(requestUserId);
        boolean isContestAdmin = requester.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException();
        }

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, participantUserId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        Boolean isBanned = request.getIsBanned();
        if (Boolean.TRUE.equals(isBanned)) {
            participant.ban();
        } else if (Boolean.FALSE.equals(isBanned)) {
            participant.unban();
        }

        return new ContestParticipantBanResponse(participant.getIsBanned());
    }

    private ContestParticipantListResponse.ParticipantItem toResponse(ContestParticipant participant) {
        User user = participant.getUser();
        return new ContestParticipantListResponse.ParticipantItem(
                user.getId(),
                user.getNickname(),
                participant.getJoinedAt(),
                participant.getScore(),
                participant.getIsBanned()
        );
    }
}
