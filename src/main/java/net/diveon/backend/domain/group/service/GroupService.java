package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupCreateRequest;
import net.diveon.backend.domain.group.dto.GroupCreateResponse;
import net.diveon.backend.domain.group.entity.Group;
import net.diveon.backend.domain.group.entity.GroupTag;
import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.entity.GroupUser.GroupRole;
import net.diveon.backend.domain.group.repository.GroupRepository;
import net.diveon.backend.domain.group.repository.GroupTagRepository;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupTagRepository groupTagRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, GroupTagRepository groupTagRepository,
                        GroupUserRepository groupUserRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.groupTagRepository = groupTagRepository;
        this.groupUserRepository = groupUserRepository;
        this.userRepository = userRepository;
    }

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
}
