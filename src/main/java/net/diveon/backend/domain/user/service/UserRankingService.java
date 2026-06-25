package net.diveon.backend.domain.user.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.user.dto.UserRankingProjection;
import net.diveon.backend.domain.user.dto.UserRankingResponse;
import net.diveon.backend.domain.user.entity.UserRankingSnapshot;
import net.diveon.backend.domain.user.repository.UserRankingSnapshotEntryRepository;
import net.diveon.backend.domain.user.repository.UserRankingSnapshotRepository;
import net.diveon.backend.global.exception.UserNotFoundException;

@Service
@Transactional(readOnly = true)
public class UserRankingService {

    private final UserRankingSnapshotRepository userRankingSnapshotRepository;
    private final UserRankingSnapshotEntryRepository userRankingSnapshotEntryRepository;

    public UserRankingService(
        UserRankingSnapshotRepository userRankingSnapshotRepository,
        UserRankingSnapshotEntryRepository userRankingSnapshotEntryRepository
    ) {
        this.userRankingSnapshotRepository = userRankingSnapshotRepository;
        this.userRankingSnapshotEntryRepository = userRankingSnapshotEntryRepository;
    }

    public UserRankingResponse getRanking(Long userId, int page) {
        UserRankingSnapshot snapshot = userRankingSnapshotRepository
            .findTopByStatusOrderByCalculatedAtDescIdDesc(UserRankingSnapshot.STATUS_COMPLETED)
            .orElseThrow(() -> new IllegalStateException("완료된 유저 랭킹 스냅샷이 없습니다."));

        Page<UserRankingProjection> resultPage = userRankingSnapshotEntryRepository
            .findRankingBySnapshotId(snapshot.getId(), PageRequest.of(page - 1, 20));
        UserRankingResponse.RankingEntry myRanking = userRankingSnapshotEntryRepository
            .findRankingBySnapshotIdAndUserId(snapshot.getId(), userId)
            .map(this::toRankingEntry)
            .orElseThrow(UserNotFoundException::new);

        List<UserRankingResponse.RankingEntry> entries = new ArrayList<>();
        for (UserRankingProjection user : resultPage.getContent()) {
            entries.add(toRankingEntry(user));
        }

        return new UserRankingResponse(
            resultPage.getTotalElements(),
            resultPage.getTotalPages(),
            page,
            snapshot.getCalculatedAt(),
            myRanking,
            entries
        );
    }

    private UserRankingResponse.RankingEntry toRankingEntry(UserRankingProjection user) {
        return new UserRankingResponse.RankingEntry(
            user.getRank(),
            user.getUserId(),
            user.getNickname(),
            user.getProfileImgUrl(),
            user.getTier(),
            user.getScore()
        );
    }
}
