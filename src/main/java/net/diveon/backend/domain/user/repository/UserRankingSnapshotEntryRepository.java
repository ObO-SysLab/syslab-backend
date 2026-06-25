package net.diveon.backend.domain.user.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.user.dto.UserRankingProjection;
import net.diveon.backend.domain.user.entity.UserRankingSnapshotEntry;

public interface UserRankingSnapshotEntryRepository extends JpaRepository<UserRankingSnapshotEntry, Long> {

    @Query(value = """
            SELECT
                e.rank AS rank,
                u.id AS userId,
                u.nickname AS nickname,
                u.profile_img_url AS profileImgUrl,
                e.tier AS tier,
                e.score AS score
            FROM user_ranking_snapshot_entry e
            JOIN domain_user u ON u.id = e.user_id
            WHERE e.snapshot_id = :snapshotId
            ORDER BY e.rank ASC, e.user_id ASC
            """,
            countQuery = "SELECT COUNT(*) FROM user_ranking_snapshot_entry WHERE snapshot_id = :snapshotId",
            nativeQuery = true)
    Page<UserRankingProjection> findRankingBySnapshotId(@Param("snapshotId") Long snapshotId, Pageable pageable);

    @Query(value = """
            SELECT
                e.rank AS rank,
                u.id AS userId,
                u.nickname AS nickname,
                u.profile_img_url AS profileImgUrl,
                e.tier AS tier,
                e.score AS score
            FROM user_ranking_snapshot_entry e
            JOIN domain_user u ON u.id = e.user_id
            WHERE e.snapshot_id = :snapshotId
              AND e.user_id = :userId
            """, nativeQuery = true)
    Optional<UserRankingProjection> findRankingBySnapshotIdAndUserId(
        @Param("snapshotId") Long snapshotId,
        @Param("userId") Long userId
    );

    @Modifying
    void deleteBySnapshotId(Long snapshotId);
}
