package net.diveon.backend.domain.group.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.group.dto.GroupRankingProjection;
import net.diveon.backend.domain.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Group g where g.id = :groupId")
    Optional<Group> findByIdForUpdate(@Param("groupId") Long groupId);

    @Query("SELECT g FROM Group g WHERE " +
           "(:tag IS NULL OR EXISTS (SELECT gt FROM GroupTag gt WHERE gt.group = g AND gt.tag = :tag)) AND " +
           "(:userId IS NULL OR EXISTS (SELECT gu FROM GroupUser gu WHERE gu.group = g AND gu.user.id = :userId))")
    Page<Group> findAllWithFilters(@Param("tag") String tag, @Param("userId") Long userId, Pageable pageable);

    Page<Group> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(value = """
            SELECT
                g.id                                                          AS groupId,
                g.title                                                       AS title,
                g.image                                                       AS image,
                COALESCE(mc.cnt, 0)                                           AS memberCount,
                (COALESCE(ps.cnt, 0) * 1
                    + COALESCE(gs.cnt, 0) * 10
                    + COALESCE(cc.cnt, 0) * 100)                             AS score
            FROM domain_group g
            LEFT JOIN (
                SELECT group_id, COUNT(*) AS cnt
                FROM group_user
                GROUP BY group_id
            ) mc ON mc.group_id = g.id
            LEFT JOIN (
                SELECT gp.group_id, COUNT(*) AS cnt
                FROM solve_submission ss
                JOIN solve_result sr ON sr.submission_id = ss.id AND sr.result_state = 'CORRECT'
                JOIN group_problem gp ON gp.problem_id = ss.problem_id
                JOIN group_user gu ON gu.group_id = gp.group_id AND gu.user_id = ss.user_id
                JOIN problem_summary p ON p.id = ss.problem_id AND p.visibility = 'public'
                GROUP BY gp.group_id
            ) ps ON ps.group_id = g.id
            LEFT JOIN (
                SELECT gp.group_id, COUNT(*) AS cnt
                FROM solve_submission ss
                JOIN solve_result sr ON sr.submission_id = ss.id AND sr.result_state = 'CORRECT'
                JOIN group_problem gp ON gp.problem_id = ss.problem_id
                JOIN group_user gu ON gu.group_id = gp.group_id AND gu.user_id = ss.user_id
                JOIN problem_summary p ON p.id = ss.problem_id AND p.visibility = 'group'
                GROUP BY gp.group_id
            ) gs ON gs.group_id = g.id
            LEFT JOIN (
                SELECT group_id, COUNT(*) AS cnt
                FROM contest
                WHERE group_id IS NOT NULL
                GROUP BY group_id
            ) cc ON cc.group_id = g.id
            ORDER BY score DESC
            """,
            countQuery = "SELECT COUNT(*) FROM domain_group",
            nativeQuery = true)
    Page<GroupRankingProjection> findGroupRanking(Pageable pageable);
}
