package net.diveon.backend.domain.user.service;

import net.diveon.backend.domain.contest.entity.ContestParticipant;
import net.diveon.backend.domain.contest.repository.ContestParticipantRepository;
import net.diveon.backend.domain.user.dto.MyContestItemResponse;
import net.diveon.backend.domain.user.dto.MyContestListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MyPageContestService {

    private static final int PAGE_SIZE = 20;

    private final ContestParticipantRepository contestParticipantRepository;

    public MyPageContestService(ContestParticipantRepository contestParticipantRepository) {
        this.contestParticipantRepository = contestParticipantRepository;
    }

    @Transactional(readOnly = true)
    public MyContestListResponse getMyContests(Long userId, int page) {
        Page<ContestParticipant> result = contestParticipantRepository.findMyContestsByUserId(userId, PageRequest.of(page - 1, PAGE_SIZE));
        List<MyContestItemResponse> items = result.getContent().stream()
                .map(MyContestItemResponse::of)
                .toList();
        return MyContestListResponse.of(result, items);
    }
}
