package net.diveon.backend.domain.vm.repository;

import net.diveon.backend.domain.vm.entity.VmContainer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VmContainerRepository extends JpaRepository<VmContainer, Long> {

    boolean existsByUserId(Long userId);

    Optional<VmContainer> findByContainerId(String containerId);
}
