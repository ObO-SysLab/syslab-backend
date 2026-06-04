package net.diveon.backend.domain.contest.service;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.contest.dto.request.ContestLeadershipTransferRequest;
import net.diveon.backend.domain.contest.dto.request.ContestParticipantBanRequest;
import net.diveon.backend.domain.contest.dto.response.ContestLeadershipTransferResponse;
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
        
        if(participantUserId == requestUserId){
                throw new ContestAccessDeniedException("그룹장은 스스로를 차단할 수 없습니다.");
        }
        
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

    @Transactional
    public ContestLeadershipTransferResponse transferContestLeadership(
            Long contestId, Long requestUserId, ContestLeadershipTransferRequest request) {
        userRepository.findById(requestUserId)
                .orElseThrow(() -> new UserNotFoundException("대회 관리자 권한 이관 요청자를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(request.getVictimUserId())
                .orElseThrow(() -> new UserNotFoundException("대회 관리자 권한 이관 대상 사용자를 찾을 수 없습니다."));

        Contest contest = contestRepository.findById(contestId)
                .orElseThrow(() -> new ContestNotFoundException("관리자 권한을 이관할 대상 대회를 찾을 수 없습니다."));

        ContestParticipant previousLeader = contestParticipantRepository.findByContestIdAndUserId(
                contestId,
                requestUserId
        ).orElseThrow(() -> new ContestAccessDeniedException("대회 관리자 권한 이관 요청자가 해당 대회의 참가자가 아닙니다."));

        boolean isContestCreator = contest.getCreatedBy().getId().equals(requestUserId);
        boolean isContestAdmin = previousLeader.getRole() == ContestParticipant.ContestRole.ADMIN;
        if (!isContestCreator || !isContestAdmin) {
            throw new ContestAccessDeniedException("대회 관리자 권한 이관은 현재 대회 관리자만 요청할 수 있습니다.");
        }

        if (Objects.equals(requestUserId, request.getVictimUserId())) {
            throw new ContestAccessDeniedException("관리자 권한은 자기 자신에게 이관할 수 없습니다.");
        }

        ContestParticipant newLeader = contestParticipantRepository.findByContestIdAndUserId(
                contestId,
                targetUser.getId()
        ).orElseThrow(() -> new ContestParticipantNotFoundException("대회 관리자 권한 이관 대상 사용자가 해당 대회의 참가자가 아닙니다."));

        if (Boolean.TRUE.equals(newLeader.getIsBanned())) {
            throw new ContestAccessDeniedException("차단된 참가자에게 관리자 권한을 이관할 수 없습니다.");
        }

        previousLeader.updateRole(ContestParticipant.ContestRole.PARTICIPANT);
        newLeader.updateRole(ContestParticipant.ContestRole.ADMIN);
        contest.transferLeadership(targetUser);

        return new ContestLeadershipTransferResponse(
                previousLeader.getUser().getNickname(),
                targetUser.getNickname()
        );
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
