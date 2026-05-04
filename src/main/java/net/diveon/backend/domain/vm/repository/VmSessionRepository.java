package net.diveon.backend.domain.vm.repository;

import net.diveon.backend.domain.vm.entity.VmSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmSessionRepository extends JpaRepository<VmSession, Long> {

    Optional<VmSession> findFirstByUserIdAndStatus(Long userId, String status);
}
