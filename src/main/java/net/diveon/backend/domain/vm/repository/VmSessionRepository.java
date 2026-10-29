package net.diveon.backend.domain.vm.repository;

import net.diveon.backend.domain.vm.entity.VmSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface VmSessionRepository extends JpaRepository<VmSession, Long> {

    Optional<VmSession> findFirstByUserIdAndStatus(Long userId, String status);

    Optional<VmSession> findFirstByUserIdAndProbIdAndStatus(Long userId, Long probId, String status);

    List<VmSession> findAllByUserIdAndStatusIn(Long userId, List<String> statuses);
    Optional<VmSession> findByIdAndUserIdAndProbId(Long id, Long userId, Long probId);
    List<VmSession> findByStatusInAndIdGreaterThanOrderByIdAsc(List<String> statuses, Long id, Pageable page);
}
