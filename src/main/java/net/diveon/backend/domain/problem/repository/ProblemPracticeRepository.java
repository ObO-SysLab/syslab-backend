package net.diveon.backend.domain.problem.repository;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProblemPracticeRepository extends JpaRepository<ProblemPractice, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE ProblemPractice p SET p.imageStatus = 'READY', p.ecrImageUri = :imageUri WHERE p.probId = :probId")
    void updateReady(@Param("probId") Long probId, @Param("imageUri") String imageUri);

    @Modifying
    @Transactional
    @Query("UPDATE ProblemPractice p SET p.imageStatus = 'FAILED' WHERE p.probId = :probId")
    void updateFailed(@Param("probId") Long probId);
}
