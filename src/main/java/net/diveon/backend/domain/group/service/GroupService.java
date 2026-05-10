package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.dto.GroupDetailResponse;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupAssignRequest.AssignRequestStatus;
import net.diveon.backend.domain.group.entity.GroupTag;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupAssignRequestRepository;
import net.diveon.backend.domain.group.repository.GroupProblemRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupTagRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupTagRepository groupTagRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupAssignRequestRepository groupAssignRequestRepository;
    private final GroupProblemRepository groupProblemRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, GroupTagRepository groupTagRepository,
                        GroupUserRepository groupUserRepository,
                        GroupAssignRequestRepository groupAssignRequestRepository,
                        GroupProblemRepository groupProblemRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupTagRepository = groupTagRepository;
        this.groupUserRepository = groupUserRepository;
        this.groupAssignRequestRepository = groupAssignRequestRepository;
        this.groupProblemRepository = groupProblemRepository;
        this.userRepository = userRepository;
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

    // 그룹 상세 조회
    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        List<String> tags = groupTagRepository.findAllByGroupId(groupId)
                .stream().map(GroupTag::getTag).toList();

        int memberCount = (int) groupUserRepository.countByGroupId(groupId);
        int problemCount = (int) groupProblemRepository.countByGroupId(groupId); 
        
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
                tags,
                new GroupDetailResponse.Stats(memberCount, problemCount),
                new GroupDetailResponse.Settings(group.getIsPrivate(), group.getIsAutoApprove()),
                new GroupDetailResponse.UserContext(myStatus, isLeader)
        );
    }
}
