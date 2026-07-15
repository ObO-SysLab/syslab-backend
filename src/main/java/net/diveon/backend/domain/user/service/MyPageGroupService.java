package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.group.entity.GroupUser;
import net.diveon.backend.domain.group.repository.GroupUserRepository;
import net.diveon.backend.domain.user.dto.MyGroupItemResponse;
import net.diveon.backend.domain.user.dto.MyGroupListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MyPageGroupService {

    private static final int PAGE_SIZE = 20;

    private final GroupUserRepository groupUserRepository;

    public MyPageGroupService(GroupUserRepository groupUserRepository) {
        this.groupUserRepository = groupUserRepository;
    }

    @Transactional(readOnly = true)
    public MyGroupListResponse getMyGroups(Long userId, int page) {
        Page<GroupUser> result = groupUserRepository.findMyGroupsByUserId(userId, PageRequest.of(page - 1, PAGE_SIZE));
        List<MyGroupItemResponse> items = result.getContent().stream()
                .map(gu -> MyGroupItemResponse.of(gu, groupUserRepository.countByGroupId(gu.getGroup().getId())))
                .toList();
        return MyGroupListResponse.of(result, items);
    }
}
