package net.diveon.backend.domain.contest.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import net.diveon.backend.domain.contest.dto.request.ContestCreateRequest;
import net.diveon.backend.domain.contest.dto.request.ContestUpdateRequest;
import net.diveon.backend.domain.contest.dto.response.ContestCreateResponse;
import net.diveon.backend.domain.contest.dto.response.ContestDetailResponse;
import net.diveon.backend.domain.contest.dto.response.ContestJoinResponse;
import net.diveon.backend.domain.contest.dto.response.ContestListResponse;
import net.diveon.backend.domain.contest.dto.response.GroupContestListResponse;
import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.entity.ContestTag;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.contest.repository.ContestProblemRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.contest.repository.ContestTagRepository;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ContestAccessDeniedException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.ContestAlreadyParticipatedException;
import net.diveon.backend.global.exception.ContestAlreadyStartedException;
import net.diveon.backend.global.exception.ContestNotFoundException;
import net.diveon.backend.global.exception.ContestParticipantNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final ContestTagRepository contestTagRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupRepository groupRepository;

    public ContestService(ContestRepository contestRepository,
                          ContestParticipantRepository contestParticipantRepository,
                          ContestTagRepository contestTagRepository,
                          ContestProblemRepository contestProblemRepository,
                          ProblemRepository problemRepository,
                          UserRepository userRepository,
                          GroupUserRepository groupUserRepository,
                          GroupRepository groupRepository) {
        this.contestRepository = contestRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.contestTagRepository = contestTagRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.groupUserRepository = groupUserRepository;
        this.groupRepository = groupRepository;
    }

    // 대회 목록 조회
    @Transactional(readOnly = true)
    public ContestListResponse getContestList(String keyword, String status, Boolean onlyJoined, int page, int size, Long userId) {
        String keywordFilter = (keyword == null) ? "" : keyword;
        String statusFilter = (status == null) ? "ALL" : status.toUpperCase();
        Long joinedUserId = (onlyJoined != null && onlyJoined && userId != null) ? userId : null; // 참여한 대회만

        Page<Contest> contestPage = contestRepository.findContestsByFilter(
                keywordFilter, statusFilter, LocalDateTime.now(), joinedUserId, PageRequest.of(page - 1, size));

        Set<Long> joinedIds = (userId != null)
                ? Set.copyOf(contestRepository.findJoinedContestIdsByUserId(userId))
                : Set.of();

        List<ContestListResponse.ContestItem> items = contestPage.getContent().stream().map(c -> {
            String contestStatus = switch (c.getStatus()) {
                case UPCOMING -> "접수 중";
                case ONGOING -> "진행 중";
                case ENDED -> "종료";
            };
            String type = c.getParticipationType() == Contest.ParticipationType.INDIVIDUAL ? "개인전" : "팀전";
            long participants = contestParticipantRepository.countByContestId(c.getId());
            return new ContestListResponse.ContestItem(
                    c.getId(), c.getTitle(), participants,
                    c.getStartTime(), c.getEndTime(),
                    type, c.getPrizeDescription(), contestStatus,
                    c.getIsHot(), joinedIds.contains(c.getId())
            );
        }).toList();

        return new ContestListResponse(contestPage.getTotalElements(), page, contestPage.getTotalPages(), items);
    }

    // 대회 상세 조회
    @Transactional(readOnly = true)
    public ContestDetailResponse getContestDetail(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);

        if (contest.getVisibility() == Contest.Visibility.GROUP) {
            if (userId == null || contest.getGroup() == null ||
                groupUserRepository.findByGroupIdAndUserId(contest.getGroup().getId(), userId).isEmpty()) {
                throw new ContestAccessDeniedException();
            }
        }

        String status = switch (contest.getStatus()) {
            case UPCOMING -> "접수 중";
            case ONGOING -> "진행 중";
            case ENDED -> "종료";
        };

        int progress = 0; // UPCOMING
        if (contest.getStatus() == Contest.ContestStatus.ONGOING) { // ONGOING
            long total = ChronoUnit.SECONDS.between(contest.getStartTime(), contest.getEndTime()); // 대회 전체 시간 (초)
            long elapsed = ChronoUnit.SECONDS.between(contest.getStartTime(), LocalDateTime.now()); // 대회 경과 시간 (초)
            progress = (int) Math.min(100, elapsed * 100 / total); // 진행률 계산식 (혹시 100 넘어가도 최대 100으로 제한)
        } else if (contest.getStatus() == Contest.ContestStatus.ENDED) {
            progress = 100; // ENDED
        }

        long totalUser = contestParticipantRepository.countByContestId(contestId);

        ContestDetailResponse.UserContext userContext = null; // 비로그인자면 그대로 null
        if (userId != null) {
            var participantOpt = contestParticipantRepository.findByContestIdAndUserId(contestId, userId);
            if (participantOpt.isPresent()) { // 참가자인경우
                var participant = participantOpt.get();
                long myRank = contestParticipantRepository.countByContestIdAndScoreGreaterThan(contestId, participant.getScore()) + 1; // 나보다 점수 높은 사람 수 + 1 = 내 순위
                userContext = new ContestDetailResponse.UserContext(
                        true,
                        participant.getRole() == ContestParticipant.ContestRole.ADMIN, // 관리자인지 그냥 참가자인지
                        participant.getScore(),
                        myRank
                );
            } else { // 참가자 아닌 경우
                userContext = new ContestDetailResponse.UserContext(false, false, 0, 0L);
            }
        }

        return new ContestDetailResponse(
                contest.getId(), contest.getTitle(), contest.getDescription(),
                contest.getStartTime(), contest.getEndTime(),
                status, progress, totalUser, userContext
        );
    }

    // 대회 생성
    @Transactional
    public ContestCreateResponse createContest(Long userId, ContestCreateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId()).orElseThrow(GroupNotFoundException::new);
            groupUserRepository.findByGroupIdAndUserId(request.getGroupId(), userId)
                    .orElseThrow(ContestAccessDeniedException::new);
        }

        Contest contest = new Contest(
                user, group,
                request.getTitle(), request.getDescription(),
                request.getContestType(), request.getParticipationType(), request.getVisibility(),
                request.getStartTime(), request.getEndTime(),
                request.getRules(), request.getPrizeDescription(), null
        );
        contestRepository.save(contest);

        contestParticipantRepository.save(new ContestParticipant(contest, user, ContestParticipant.ContestRole.ADMIN));

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<ContestTag> tags = request.getTags().stream()
                    .map(tag -> new ContestTag(contest, tag))
                    .toList();
            contestTagRepository.saveAll(tags);
        }

        return new ContestCreateResponse(contest.getId(), contest.getTitle());
    }

    // 대회 참가
    @Transactional
    public ContestJoinResponse joinContest(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 시작 전인 대회만 참가 가능
        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ContestAlreadyStartedException();
        }

        // 그룹 전용 대회면 그룹원인지 확인
        if (contest.getVisibility() == Contest.Visibility.GROUP) {
            if (contest.getGroup() == null ||
                groupUserRepository.findByGroupIdAndUserId(contest.getGroup().getId(), userId).isEmpty()) {
                throw new ContestAccessDeniedException();
            }
        }

        // 이미 참가 중인지 확인
        if (contestParticipantRepository.findByContestIdAndUserId(contestId, userId).isPresent()) {
            throw new ContestAlreadyParticipatedException();
        }

        // 참가
        contestParticipantRepository.save(new ContestParticipant(contest, user, ContestParticipant.ContestRole.PARTICIPANT));

        // 50명 넘는 대회는 Hot으로 표기 (임시)
        long participantCount = contestParticipantRepository.countByContestId(contestId);
        if (participantCount >= 50) {
            contest.updateIsHot(true);
        }

        return new ContestJoinResponse(true);
    }

    // 대회 설정 수정
    @Transactional
    public void updateContest(Long contestId, Long userId, ContestUpdateRequest request) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);

        contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .filter(p -> p.getRole() == ContestParticipant.ContestRole.ADMIN)
                .orElseThrow(ContestAccessDeniedException::new);

        contest.updateTitleAndDescription(request.getTitle(), request.getDescription());

        if (contest.getStatus() == Contest.ContestStatus.UPCOMING) {
            contest.updateTime(request.getStartTime(), request.getEndTime());
        }
    }

    // 대회 삭제
    @Transactional
    public void deleteContest(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);

        contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .filter(p -> p.getRole() == ContestParticipant.ContestRole.ADMIN)
                .orElseThrow(ContestAccessDeniedException::new);

        contestProblemRepository.findAllByContestId(contestId).forEach(cp -> {
            if ("contest".equals(cp.getProblem().getVisibility())) {
                problemRepository.delete(cp.getProblem());
            }
        });

        contestRepository.delete(contest);
    }

    // 대회 참가 취소
    @Transactional
    public ContestJoinResponse leaveContest(Long contestId, Long userId) {
        Contest contest = contestRepository.findById(contestId).orElseThrow(ContestNotFoundException::new);

        if (contest.getStatus() != Contest.ContestStatus.UPCOMING) {
            throw new ContestAlreadyStartedException();
        }

        ContestParticipant participant = contestParticipantRepository.findByContestIdAndUserId(contestId, userId)
                .orElseThrow(ContestParticipantNotFoundException::new);

        if (participant.getRole() == ContestParticipant.ContestRole.ADMIN) {
            throw new ContestAccessDeniedException();
        }

        contestParticipantRepository.delete(participant);

        return new ContestJoinResponse(false);
    }

    // 그룹 전용 대회 목록 조회
    @Transactional(readOnly = true)
    public GroupContestListResponse getGroupContestList(Long groupId, Long userId, int page, int size) {
        groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);

        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(ContestAccessDeniedException::new);

        Page<Contest> contestPage = contestRepository.findGroupContestsByGroupId(groupId, PageRequest.of(page - 1, size));

        List<GroupContestListResponse.ContestItem> items = contestPage.getContent().stream().map(c -> {
            String status = switch (c.getStatus()) {
                case UPCOMING -> "접수 중";
                case ONGOING -> "진행 중";
                case ENDED -> "종료";
            };
            long participantCount = contestParticipantRepository.countByContestId(c.getId());
            return new GroupContestListResponse.ContestItem(
                    c.getId(), c.getTitle(), c.getCreatedBy().getNickname(),
                    c.getStartTime(), c.getEndTime(), status, participantCount
            );
        }).toList();

        return new GroupContestListResponse(page, contestPage.getTotalPages(), items);
    }
}
