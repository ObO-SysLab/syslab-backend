package net.diveon.backend.domain.group.service;

import net.diveon.backend.domain.group.dto.GroupRankingProjection;
import net.diveon.backend.domain.group.dto.GroupRankingResponse;
import net.diveon.backend.domain.group.repository.GroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GroupRankingService {

    private final GroupRepository groupRepository;

    public GroupRankingService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public GroupRankingResponse getRanking(int page) {
        Page<GroupRankingProjection> resultPage = groupRepository.findGroupRanking(PageRequest.of(page - 1, 20));

        int startRank = (page - 1) * 20 + 1;
        List<GroupRankingResponse.RankingEntry> entries = new ArrayList<>();
        int rank = startRank;

        for (GroupRankingProjection proj : resultPage.getContent()) {
            entries.add(new GroupRankingResponse.RankingEntry(
                    rank++,
                    proj.getGroupId(),
                    proj.getTitle(),
                    proj.getImage(),
                    proj.getMemberCount(),
                    proj.getScore()
            ));
        }

        return new GroupRankingResponse(
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                page,
                entries
        );
    }
}
