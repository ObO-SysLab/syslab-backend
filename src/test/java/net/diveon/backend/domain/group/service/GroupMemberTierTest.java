package net.diveon.backend.domain.group.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.repository.GroupAssignRequestRepository;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class GroupMemberTierTest {
    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupUserRepository groupUsers = mock(GroupUserRepository.class);
    private final GroupMemberService service = new GroupMemberService(groups, groupUsers,
            mock(GroupAssignRequestRepository.class), mock(UserRepository.class));

    private User user(long id, String nickname, Integer tier, Integer score, String image) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", nickname);
        ReflectionTestUtils.setField(user, "tier", tier);
        ReflectionTestUtils.setField(user, "score", score);
        ReflectionTestUtils.setField(user, "profileImgUrl", image);
        return user;
    }

    @Test
    void membersCarrySiteTierScoreAndPhoto() {
        Group group = new Group();
        when(groups.findById(12L)).thenReturn(Optional.of(group));
        var leader = new GroupUser(group, user(1L, "leader", 2, 240, "https://cdn/1"), GroupUser.GroupRole.LEADER);
        var member = new GroupUser(group, user(2L, "member", 1, 0, null), GroupUser.GroupRole.MEMBER);
        when(groupUsers.findMembersByGroupIdAndKeyword(eq(12L), any(), any()))
                .thenReturn(new PageImpl<>(List.of(leader, member), PageRequest.of(0, 50), 2));

        var members = service.getMembers(12L, 1, 50, null).getMembers();

        assertEquals(2, members.size());
        assertEquals("그룹장", members.get(0).getRole());
        assertEquals(2, members.get(0).getTier());
        assertEquals(240, members.get(0).getScore());
        assertEquals("https://cdn/1", members.get(0).getProfileImgUrl());
        assertEquals(1, members.get(1).getTier());
        assertEquals(0, members.get(1).getScore());
        assertNull(members.get(1).getProfileImgUrl());
    }
}
