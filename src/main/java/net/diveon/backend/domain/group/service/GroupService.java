package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupAddProblemsRequest;
import net.diveon.backend.domain.group.dto.GroupUpdateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.dto.GroupDetailResponse;
import net.diveon.backend.domain.group.dto.GroupListResponse;
import net.diveon.backend.domain.group.dto.GroupMyListResponse;
import net.diveon.backend.domain.group.dto.GroupProblemListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupAssignRequest.AssignRequestStatus;
import net.diveon.backend.domain.group.entity.GroupProblem;
import net.diveon.backend.domain.group.entity.GroupTag;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupAssignRequestRepository;
import net.diveon.backend.domain.group.repository.GroupProblemRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupTagRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.problem.service.ProblemDeleteService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.GroupAccessDeniedException;
import net.diveon.backend.global.exception.GroupLeaderPermissionDeniedException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.GroupProblemAlreadyExistsException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import net.diveon.backend.global.s3.ImageUploadService;
import net.diveon.backend.global.util.ImageFileValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupTagRepository groupTagRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupAssignRequestRepository groupAssignRequestRepository;
    private final GroupProblemRepository groupProblemRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final ContestRepository contestRepository;
    private final ProblemDeleteService problemDeleteService;
    private final ImageUploadService imageUploadService;

    public GroupService(GroupRepository groupRepository, GroupTagRepository groupTagRepository,
                        GroupUserRepository groupUserRepository,
                        GroupAssignRequestRepository groupAssignRequestRepository,
                        GroupProblemRepository groupProblemRepository,
                        UserRepository userRepository,
                        ProblemRepository problemRepository,
                        ProblemPracticeRepository problemPracticeRepository,
                        ContestRepository contestRepository,
                        ProblemDeleteService problemDeleteService,
                        ImageUploadService imageUploadService) {
        this.groupRepository = groupRepository;
        this.groupTagRepository = groupTagRepository;
        this.groupUserRepository = groupUserRepository;
        this.groupAssignRequestRepository = groupAssignRequestRepository;
        this.groupProblemRepository = groupProblemRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.contestRepository = contestRepository;
        this.problemDeleteService = problemDeleteService;
        this.imageUploadService = imageUploadService;
    }

    // 그룹 목록 조회
    @Transactional(readOnly = true)
    public GroupListResponse getGroupList(Long userId, int page, int size, String tag, Boolean isJoined) {
        // 비 로그인자가 '내 그룹만 보기' 버튼 누르면 401 오류 던짐
        if (Boolean.TRUE.equals(isJoined) && userId == null) {
            throw new UserNotFoundException();
        }
        // isJoined=true면 내 그룹만, 아니면 전체 조회
        Long filterUserId = Boolean.TRUE.equals(isJoined) ? userId : null;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Group> groupPage = groupRepository.findAllWithFilters(tag, filterUserId, pageable);
        return buildGroupListResponse(groupPage, userId, page);
    }

    // 그룹 검색
    @Transactional(readOnly = true)
    public GroupListResponse searchGroups(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Group> groupPage = groupRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        return buildGroupListResponse(groupPage, userId, page);
    }

    // N+1 방지: groupIds로 태그/멤버/joined 여부 일괄 조회 후 응답 조립
    private GroupListResponse buildGroupListResponse(Page<Group> groupPage, Long userId, int page) {
        List<Long> groupIds = groupPage.getContent().stream().map(Group::getId).toList();

        // 1. 태그 목록
        Map<Long, List<String>> tagMap = groupTagRepository.findAllByGroupIdIn(groupIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        gt -> gt.getGroup().getId(),
                        java.util.stream.Collectors.mapping(GroupTag::getTag, java.util.stream.Collectors.toList())
                ));

        List<GroupUser> allGroupUsers = groupUserRepository.findAllByGroupIdIn(groupIds);

        // 2. 멤버수
        Map<Long, Long> memberCountMap = allGroupUsers.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        gu -> gu.getGroup().getId(),
                        java.util.stream.Collectors.counting()
                ));

        // 3. 내가 속한 그룹 ID 목록
        java.util.Set<Long> joinedGroupIds = userId == null ? java.util.Set.of() :
                allGroupUsers.stream()
                        .filter(gu -> gu.getUser().getId().equals(userId))
                        .map(gu -> gu.getGroup().getId())
                        .collect(java.util.stream.Collectors.toSet());

        List<GroupListResponse.GroupItem> groups = groupPage.getContent().stream()
                .map(group -> new GroupListResponse.GroupItem(
                        group.getId(),
                        group.getTitle(),
                        group.getLeader().getNickname(),
                        memberCountMap.getOrDefault(group.getId(), 0L).intValue(),
                        group.getLimitMemberCount(),
                        tagMap.getOrDefault(group.getId(), List.of()),
                        joinedGroupIds.contains(group.getId())
                )).toList();

        return new GroupListResponse(groupPage.getTotalElements(), groupPage.getTotalPages(), page, groups);
    }

    // 그룹 설정 수정
    @Transactional
    public void updateGroup(Long groupId, Long userId, GroupUpdateRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        if (groupUser.getRole() != GroupUser.GroupRole.LEADER) {
            throw new GroupAccessDeniedException();
        }

        group.update(request.getTitle(), request.getDescription(), request.getIsPrivate(), request.getIsAutoApprove());

        // 태그 전체 교체 (기존 태그 삭제 후 새로 저장)
        groupTagRepository.deleteAllByGroupId(groupId);
        List<String> tags = request.getTags();
        if (tags != null) {
            for (String tag : tags) {
                groupTagRepository.save(new GroupTag(group, tag));
            }
        }
    }

    // 그룹 이미지 업로드 (그룹장만 가능)
    @Transactional
    public String uploadGroupImage(Long groupId, Long userId, MultipartFile image) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        if (groupUser.getRole() != GroupUser.GroupRole.LEADER) {
            throw new GroupAccessDeniedException();
        }

        String extension = ImageFileValidator.validateAndGetExtension(image);
        String key = "groups/" + groupId + "." + extension;
        String imageUrl = imageUploadService.upload(key, image);

        group.updateImage(imageUrl);
        return imageUrl;
    }

    // 그룹 삭제
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        if (groupUser.getRole() != GroupUser.GroupRole.LEADER) {
            throw new GroupAccessDeniedException();
        }

        groupProblemRepository.findAllByGroupId(groupId).forEach(gp -> {
            if ("group".equals(gp.getProblem().getVisibility())) {
                problemRepository.delete(gp.getProblem());
            }
        });

        groupRepository.delete(group);
    }

    // 그룹 생성
    @Transactional
    public GroupCreateResponse createGroup(Long userId, GroupCreateRequest request) {
        User leader = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Group group = new Group(
                leader,
                null, // limitMemberCount → 기본값 50
                null, // image → 이미지 업로드 추후 구현
                request.getTitle(),
                request.getDescription(),
                request.getIsPrivate(),
                request.getIsAutoApprove(),
                null // invitationCode → 초대코드 추후 구현
        );
        groupRepository.save(group);

        List<String> tags = request.getTags();
        if (tags != null) {
            for (String tag : tags) {
                groupTagRepository.save(new GroupTag(group, tag));
            }
        }

        groupUserRepository.save(new GroupUser(group, leader, GroupRole.LEADER));

        return new GroupCreateResponse(group.getId());
    }


    // 그룹 문제 목록 조회
    @Transactional(readOnly = true)
    public GroupProblemListResponse getGroupProblems(Long groupId, Long userId, int page) {
        groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        groupUserRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(GroupAccessDeniedException::new);

        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by(Sort.Direction.DESC, "assignedAt"));
        Page<GroupProblem> groupProblemPage = groupProblemRepository.findAllByGroupId(groupId, pageable);

        Set<Long> practiceProblemIds = groupProblemPage.getContent().stream()
                .map(GroupProblem::getProblem)
                .filter(p -> "practice".equals(p.getType()))
                .map(Problem::getId)
                .collect(Collectors.toSet());

        Set<Long> readyPracticeIds = practiceProblemIds.isEmpty() ? Set.of() :
                problemPracticeRepository.findAllById(practiceProblemIds).stream()
                        .filter(pp -> "READY".equals(pp.getImageStatus()))
                        .map(ProblemPractice::getProbId)
                        .collect(Collectors.toSet());

        List<GroupProblemListResponse.ProblemItem> problems = groupProblemPage.getContent()
                .stream()
                .filter(gp -> {
                    if (!"practice".equals(gp.getProblem().getType())) return true;
                    return readyPracticeIds.contains(gp.getProblem().getId());
                })
                .map(GroupProblemListResponse.ProblemItem::of)
                .toList();

        return new GroupProblemListResponse(
                problems,
                page,
                groupProblemPage.getTotalPages()
        );
    }

    // 공개 문제 그룹에 추가
    @Transactional
    public void addProblems(Long groupId, Long userId, GroupAddProblemsRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        Long problemId = request.getProblemId();
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        if (!"public".equals(problem.getVisibility())) {
            throw new GroupAccessDeniedException();
        }

        if (groupProblemRepository.existsByGroupIdAndProblemId(groupId, problemId)) {
            throw new GroupProblemAlreadyExistsException();
        }

        groupProblemRepository.save(new GroupProblem(problem, group));
    }

    // 그룹 문제 삭제
    @Transactional
    public void deleteGroupProblem(Long groupId, Long problemId, Long userId) {
        groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);

        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupAccessDeniedException::new);

        if (groupUser.getRole() != GroupUser.GroupRole.LEADER) {
            throw new GroupLeaderPermissionDeniedException();
        }

        GroupProblem groupProblem = groupProblemRepository.findByGroupIdAndProblemId(groupId, problemId)
                .orElseThrow(ProblemNotFoundException::new);

        Problem problem = groupProblem.getProblem();
        groupProblemRepository.delete(groupProblem);

        if ("group".equals(problem.getVisibility())) {
            problemDeleteService.deleteProblemByType(problem.getId(), problem.getType());
        }
    }

    // 내가 속한 그룹 목록 조회
    /* findAllByUserId(userId) 로 내 userId로 조회하면 내가 속한 그룹들이 List<GroupUser> 형태로 나오고,
      각각에서 group.getId(), group.getTitle() 뽑아서 반환 */
    @Transactional(readOnly = true)
    public List<GroupMyListResponse> getMyGroups(Long userId, Long problemId) {
        List<GroupUser> groupUsers = groupUserRepository.findAllByUserId(userId);
        return groupUsers.stream()
                .map(groupUser -> {
                    boolean isAlreadyAdded = problemId != null &&
                            groupProblemRepository.existsByGroupIdAndProblemId(groupUser.getGroup().getId(), problemId);
                    return GroupMyListResponse.of(groupUser, isAlreadyAdded);
                })
                .toList();
    }

    // 그룹 상세 조회
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        List<String> tags = groupTagRepository.findAllByGroupId(groupId)
                .stream().map(GroupTag::getTag).toList();

        int memberCount = (int) groupUserRepository.countByGroupId(groupId);
        int problemCount = (int) groupProblemRepository.countByGroupId(groupId);
        int contestCount = (int) contestRepository.countByGroupId(groupId);
        
        // 로그인 안 했을 경우
        String myStatus = "none";
        boolean isLeader = false;

        // 로그인 했을 경우
        if (userId != null) {
            Optional<GroupUser> groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId);
            if (groupUser.isPresent()) {
                myStatus = "member";
                isLeader = groupUser.get().getRole() == GroupRole.LEADER;
            } else if (groupAssignRequestRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, AssignRequestStatus.PENDING)) {
                myStatus = "pending";
            }
        }

        return new GroupDetailResponse(
                group.getId(),
                group.getTitle(),
                group.getDescription(),
                group.getImage(),
                tags,
                new GroupDetailResponse.Stats(memberCount, problemCount, contestCount),
                new GroupDetailResponse.Settings(group.getIsPrivate(), group.getIsAutoApprove()),
                new GroupDetailResponse.UserContext(myStatus, isLeader)
        );
    }
}
