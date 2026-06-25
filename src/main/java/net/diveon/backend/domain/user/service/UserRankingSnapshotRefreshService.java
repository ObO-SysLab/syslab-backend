package net.diveon.backend.domain.user.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.diveon.backend.domain.user.entity.UserRankingSnapshot;
import net.diveon.backend.domain.user.repository.UserRankingSnapshotEntryRepository;
import net.diveon.backend.domain.user.repository.UserRankingSnapshotRepository;

@Service
public class UserRankingSnapshotRefreshService {

    private static final int SNAPSHOT_RETAIN_COUNT = 2;

    private final UserRankingSnapshotRepository userRankingSnapshotRepository;
    private final UserRankingSnapshotEntryRepository userRankingSnapshotEntryRepository;
    private final JdbcTemplate jdbcTemplate;

    public UserRankingSnapshotRefreshService(
        UserRankingSnapshotRepository userRankingSnapshotRepository,
        UserRankingSnapshotEntryRepository userRankingSnapshotEntryRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.userRankingSnapshotRepository = userRankingSnapshotRepository;
        this.userRankingSnapshotEntryRepository = userRankingSnapshotEntryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(
        fixedDelayString = "${ranking.user.snapshot-refresh-ms:300000}",
        initialDelayString = "${ranking.user.snapshot-initial-delay-ms:0}"
    )
    @Transactional
    public void refreshSnapshot() {
        UserRankingSnapshot snapshot = userRankingSnapshotRepository.saveAndFlush(
            new UserRankingSnapshot(LocalDateTime.now())
        );

        jdbcTemplate.update("""
            INSERT INTO user_ranking_snapshot_entry (snapshot_id, user_id, rank, tier, score)
            SELECT
                ?,
                u.id,
                CAST(DENSE_RANK() OVER (ORDER BY COALESCE(u.score, 0) DESC) AS INTEGER),
                COALESCE(u.tier, 0),
                COALESCE(u.score, 0)
            FROM domain_user u
            ORDER BY COALESCE(u.score, 0) DESC, u.id ASC
            """, snapshot.getId());

        snapshot.complete();
        deleteOldCompletedSnapshots();
    }

    private void deleteOldCompletedSnapshots() {
        List<UserRankingSnapshot> completedSnapshots =
            userRankingSnapshotRepository.findByStatusOrderByCalculatedAtDescIdDesc(UserRankingSnapshot.STATUS_COMPLETED);

        for (int i = SNAPSHOT_RETAIN_COUNT; i < completedSnapshots.size(); i++) {
            UserRankingSnapshot snapshot = completedSnapshots.get(i);
            userRankingSnapshotEntryRepository.deleteBySnapshotId(snapshot.getId());
            userRankingSnapshotRepository.delete(snapshot);
        }
    }
}
