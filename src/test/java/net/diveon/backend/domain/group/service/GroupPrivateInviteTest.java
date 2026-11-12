package net.diveon.backend.domain.group.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import net.diveon.backend.domain.contest.repository.ContestRepository;
import net.diveon.backend.domain.group.dto.GroupUpdateRequest;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.repository.GroupAssignRequestRepository;
import net.diveon.backend.domain.group.repository.GroupProblemRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupTagRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.problem.service.ProblemDeleteService;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.s3.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// QA 5번: 공개로 만든 그룹을 설정에서 비공개로 바꾸면 초대 링크가 생기지 않았다.
class GroupPrivateInviteTest {
    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupUserRepository groupUsers = mock(GroupUserRepository.class);
    private final GroupService service = new GroupService(groups, mock(GroupTagRepository.class), groupUsers,
            mock(GroupAssignRequestRepository.class), mock(GroupProblemRepository.class), mock(UserRepository.class),
            mock(ProblemRepository.class), mock(ProblemPracticeRepository.class), mock(ContestRepository.class),
            mock(ProblemDeleteService.class), mock(ImageUploadService.class));

    private Group group(boolean isPrivate, String code, LocalDateTime expiresAt) {
        User leader = new User();
        ReflectionTestUtils.setField(leader, "id", 1L);
        Group group = new Group(leader, null, null, "QA 그룹", "d", isPrivate, false, code, expiresAt);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(groupUsers.findByGroupIdAndUserId(7L, 1L)).thenReturn(Optional.of(new GroupUser(group, leader, GroupUser.GroupRole.LEADER)));
        return group;
    }

    private GroupUpdateRequest request(boolean isPrivate) {
        var request = new GroupUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "QA 그룹");
        ReflectionTestUtils.setField(request, "description", "d");
        ReflectionTestUtils.setField(request, "isPrivate", isPrivate);
        ReflectionTestUtils.setField(request, "isAutoApprove", false);
        return request;
    }

    @Test
    void switchingToPrivateIssuesAnInviteCode() {
        Group group = group(false, null, null);
        service.updateGroup(7L, 1L, request(true));
        assertNotNull(group.getInvitationCode());
        assertTrue(group.getInvitationCodeExpiresAt().isAfter(LocalDateTime.now().plusDays(29)));
    }

    @Test
    void savingAPrivateGroupKeepsAUsableCode() {
        LocalDateTime expires = LocalDateTime.now().plusDays(3);
        Group group = group(true, "keep-me", expires);
        service.updateGroup(7L, 1L, request(true));
        assertEquals("keep-me", group.getInvitationCode());
        assertEquals(expires, group.getInvitationCodeExpiresAt());
    }

    @Test
    void savingAPrivateGroupReplacesAnExpiredCode() {
        Group group = group(true, "old", LocalDateTime.now().minusDays(1));
        service.updateGroup(7L, 1L, request(true));
        assertNotEquals("old", group.getInvitationCode());
        assertTrue(group.getInvitationCodeExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void publicGroupsGetNoNewCode() {
        Group group = group(false, null, null);
        service.updateGroup(7L, 1L, request(false));
        assertNull(group.getInvitationCode());
    }
}
