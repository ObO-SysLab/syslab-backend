package net.diveon.backend.domain.contest.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.contest.entity.Contest;
import net.diveon.backend.domain.contest.entity.ContestParticipant;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    @Query("SELECT c FROM Contest c WHERE " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:statusFilter = 'ALL' OR " +
           "  (:statusFilter = 'UPCOMING' AND c.startTime > :now) OR " +
           "  (:statusFilter = 'ONGOING' AND c.startTime <= :now AND c.endTime >= :now) OR " +
           "  (:statusFilter = 'ENDED' AND c.endTime < :now)) AND " +
           "(:joinedUserId IS NULL OR EXISTS (SELECT p FROM ContestParticipant p WHERE p.contest = c AND p.user.id = :joinedUserId))")
           
    Page<Contest> findContestsByFilter(@Param("keyword") String keyword,
                                       @Param("statusFilter") String statusFilter,
                                       @Param("now") LocalDateTime now,
                                       @Param("joinedUserId") Long joinedUserId,
                                       Pageable pageable);

    @Query("SELECT p.contest.id FROM ContestParticipant p WHERE p.user.id = :userId")
    List<Long> findJoinedContestIdsByUserId(@Param("userId") Long userId);
}
