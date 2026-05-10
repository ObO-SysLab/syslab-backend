package net.diveon.backend.domain.group.service;

import java.util.Optional;

import net.diveon.backend.domain.group.dto.GroupMemberCommonResponse;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupAssignRequest;
import net.diveon.backend.domain.group.entity.GroupAssignRequest.AssignRequestStatus;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupAssignRequestRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.GroupAssignRequestNotPendingException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupMemberService {

    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final GroupAssignRequestRepository groupAssignRequestRepository;
    private final UserRepository userRepository;

    public GroupMemberService(GroupRepository groupRepository,
                              GroupUserRepository groupUserRepository,
                              GroupAssignRequestRepository groupAssignRequestRepository,
                              UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupUserRepository = groupUserRepository;
        this.groupAssignRequestRepository = groupAssignRequestRepository;
        this.userRepository = userRepository;
    }

    // 그룹 가입 신청
    @Transactional
    public GroupMemberCommonResponse applyGroupMembership(Long groupId, Long userId) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(GroupNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Optional<GroupUser> existingMember = groupUserRepository.findByGroupIdAndUserId(groupId, userId);
        if (existingMember.isPresent()) {
            return new GroupMemberCommonResponse(userId, "member");
        }

        boolean hasPendingRequest = groupAssignRequestRepository.existsByGroupIdAndUserIdAndStatus(
                groupId,
                userId,
                AssignRequestStatus.PENDING
        );
        if (hasPendingRequest) {
            return new GroupMemberCommonResponse(userId, "pending");
        }

        GroupAssignRequest assignRequest = new GroupAssignRequest(group, user);
        groupAssignRequestRepository.save(assignRequest);

        if (Boolean.TRUE.equals(group.getIsAutoApprove())) {
            validateGroupCapacity(groupId, group);
            assignRequest.approve(group.getLeader(), "auto approved");
            groupUserRepository.save(new GroupUser(group, user, GroupRole.MEMBER));
            return new GroupMemberCommonResponse(userId, "APPROVED");
        }

        return new GroupMemberCommonResponse(userId, "pending");
    }

    // 그룹 가입 신청 철회
    @Transactional
    public GroupMemberCommonResponse cancelPendingGroupMembership(Long groupId, Long userId) {
        groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        GroupAssignRequest assignRequest = groupAssignRequestRepository
                .findFirstByGroupIdAndUserIdOrderByAppliedAtDescIdDesc(groupId, userId)
                .orElseThrow(GroupAssignRequestNotPendingException::new);

        if (assignRequest.getStatus() != AssignRequestStatus.PENDING) {
            throw new GroupAssignRequestNotPendingException();
        }

        assignRequest.cancel("canceled by applicant");
        return new GroupMemberCommonResponse(userId, "CANCELED");
    }

    private void validateGroupCapacity(Long groupId, Group group) {
        long memberCount = groupUserRepository.countByGroupId(groupId);
        if (memberCount >= group.getLimitMemberCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "그룹 최대 인원수를 초과할 수 없습니다.");
        }
    }
}
