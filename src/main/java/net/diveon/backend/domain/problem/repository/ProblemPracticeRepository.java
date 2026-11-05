package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProblemPracticeRepository extends JpaRepository<ProblemPractice, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProblemPractice p where p.probId = :id")
    Optional<ProblemPractice> findBuildForUpdate(@Param("id") Long id);

    @Query("select p from ProblemPractice p where p.imageStatus = 'PENDING' and p.buildRequestId is not null and p.probId > :afterId order by p.probId")
    List<ProblemPractice> findPendingBuilds(@Param("afterId") Long afterId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update ProblemPractice p set p.imageStatus = 'FAILED', p.buildFailureReason = 'DEADLINE_EXCEEDED' where p.imageStatus = 'PENDING' and p.buildRequestId is not null and p.buildDeadlineAt <= :now")
    int expireBuilds(@Param("now") Instant now);
}
