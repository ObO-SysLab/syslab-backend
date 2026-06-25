package net.diveon.backend.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.user.entity.UserRankingSnapshot;

public interface UserRankingSnapshotRepository extends JpaRepository<UserRankingSnapshot, Long> {

    Optional<UserRankingSnapshot> findTopByStatusOrderByCalculatedAtDescIdDesc(String status);

    List<UserRankingSnapshot> findByStatusOrderByCalculatedAtDescIdDesc(String status);
}
