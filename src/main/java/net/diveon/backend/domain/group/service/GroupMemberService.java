package net.diveon.backend.domain.group.service;

import java.util.List;
import java.util.Optional;

import net.diveon.backend.domain.group.dto.GroupAssignDecisionRequest;
import net.diveon.backend.domain.group.dto.GroupMemberKickResponse;
import net.diveon.backend.domain.group.dto.GroupMemberCommonResponse;
import net.diveon.backend.domain.group.dto.GroupPendingMemberListResponse;
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
import net.diveon.backend.global.exception.GroupLeaderCannotLeaveException;
import net.diveon.backend.global.exception.GroupLeaderPermissionDeniedException;
import net.diveon.backend.global.exception.GroupNotFoundException;
import net.diveon.backend.global.exception.GroupUserNotFoundException;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    // 그룹 탈퇴
    @Transactional
    public GroupMemberCommonResponse leaveGroup(Long groupId, Long userId) {
        groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        GroupUser groupUser = groupUserRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(GroupUserNotFoundException::new);
        //양도 만약에 생각하면 이게 더 좋은 검사인듯? 바꿀수는 있습니다 userId랑 그룹장 leaderId 검사하게
        // 커밋용 추가
        if (groupUser.getRole() == GroupRole.LEADER) {
            throw new GroupLeaderCannotLeaveException();
        }

        groupUserRepository.delete(groupUser);
        return new GroupMemberCommonResponse(userId, "none");
    }

    // 그룹 멤버 강제 퇴장
    @Transactional
    public GroupMemberKickResponse kickGroupMember(Long groupId, Long requesterId, Long targetUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        userRepository.findById(requesterId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        GroupUser requesterGroupUser = groupUserRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(GroupLeaderPermissionDeniedException::new);
        validateGroupLeader(group, requesterId, requesterGroupUser);

        GroupUser targetGroupUser = groupUserRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(GroupUserNotFoundException::new);

        if (targetGroupUser.getRole() == GroupRole.LEADER) {
            throw new GroupLeaderCannotLeaveException();
        }

        groupUserRepository.delete(targetGroupUser);
        return new GroupMemberKickResponse(targetUserId);
    }

    // 그룹 가입 신청 승인
    @Transactional
    public GroupMemberCommonResponse acceptPendingGroupMembership(Long groupId, Long requesterId, Long targetUserId,
                                                                  GroupAssignDecisionRequest request) {
        Group group = groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(GroupNotFoundException::new);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(UserNotFoundException::new);
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        GroupUser requesterGroupUser = groupUserRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(GroupLeaderPermissionDeniedException::new);
        validateGroupLeader(group, requesterId, requesterGroupUser);

        GroupAssignRequest assignRequest = groupAssignRequestRepository
                .findByGroupIdAndUserIdAndStatus(groupId, targetUserId, AssignRequestStatus.PENDING)
                .orElseThrow(GroupAssignRequestNotPendingException::new);

        Optional<GroupUser> existingMember = groupUserRepository.findByGroupIdAndUserId(groupId, targetUserId);
        if (existingMember.isPresent()) {
            return new GroupMemberCommonResponse(targetUserId, "member");
        }

        validateGroupCapacity(groupId, group);
        assignRequest.approve(requester, resolveDecisionReason(request));
        groupUserRepository.save(new GroupUser(group, targetUser, GroupRole.MEMBER));
        return new GroupMemberCommonResponse(targetUserId, "member");
    }

    // 그룹 가입 신청 거절
    @Transactional
    public GroupMemberCommonResponse rejectPendingGroupMembership(Long groupId, Long requesterId, Long targetUserId,
                                                                  GroupAssignDecisionRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        User requester = userRepository.findById(requesterId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.findById(targetUserId)
                .orElseThrow(UserNotFoundException::new);

        GroupUser requesterGroupUser = groupUserRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(GroupLeaderPermissionDeniedException::new);
        validateGroupLeader(group, requesterId, requesterGroupUser);

        GroupAssignRequest assignRequest = groupAssignRequestRepository
                .findByGroupIdAndUserIdAndStatus(groupId, targetUserId, AssignRequestStatus.PENDING)
                .orElseThrow(GroupAssignRequestNotPendingException::new);

        assignRequest.reject(requester, resolveDecisionReason(request));
        return new GroupMemberCommonResponse(targetUserId, "rejected");
    }

    // 그룹 가입 대기자 목록 조회
    @Transactional(readOnly = true)
    public GroupPendingMemberListResponse getPendingMembers(Long groupId, Long requesterId, int page, int size) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);
        userRepository.findById(requesterId)
                .orElseThrow(UserNotFoundException::new);

        GroupUser requesterGroupUser = groupUserRepository.findByGroupIdAndUserId(groupId, requesterId)
                .orElseThrow(GroupLeaderPermissionDeniedException::new);
        validateGroupLeader(group, requesterId, requesterGroupUser);

        int currentPage = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        PageRequest pageRequest = PageRequest.of(
                currentPage - 1,
                pageSize,
                Sort.by(Sort.Order.asc("appliedAt"), Sort.Order.asc("id"))
        );
        Page<GroupAssignRequest> pendingRequestPage = groupAssignRequestRepository.findByGroupIdAndStatus(
                groupId,
                AssignRequestStatus.PENDING,
                pageRequest
        );

        List<GroupPendingMemberListResponse.PendingMember> pendingMembers = pendingRequestPage.getContent()
                .stream()
                .map(assignRequest -> new GroupPendingMemberListResponse.PendingMember(
                        assignRequest.getUser().getId(),
                        assignRequest.getUser().getNickname(),
                        assignRequest.getAppliedAt()
                ))
                .toList();

        return new GroupPendingMemberListResponse(
                pendingRequestPage.getTotalElements(),
                pendingRequestPage.getTotalPages(),
                currentPage,
                pendingMembers
        );
    }

    private void validateGroupLeader(Group group, Long requesterId, GroupUser requesterGroupUser) {
        boolean isGroupLeaderId = group.getLeader().getId().equals(requesterId);
        boolean isLeaderRole = requesterGroupUser.getRole() == GroupRole.LEADER;
        if (!isGroupLeaderId || !isLeaderRole) {
            throw new GroupLeaderPermissionDeniedException();
        }
    }

    private void validateGroupCapacity(Long groupId, Group group) {
        long memberCount = groupUserRepository.countByGroupId(groupId);
        if (memberCount >= group.getLimitMemberCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "그룹 최대 인원수를 초과할 수 없습니다.");
        }
    }

    private String resolveDecisionReason(GroupAssignDecisionRequest request) {
        if (request == null || request.getDecidedReason() == null || request.getDecidedReason().isBlank()) {
            return "No Reason";
        }
        return request.getDecidedReason();
    }
}
