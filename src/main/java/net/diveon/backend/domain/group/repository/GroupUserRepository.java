package net.diveon.backend.domain.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupUser;

import java.util.List;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {
    long countByGroupId(Long groupId);
    Optional<GroupUser> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupUser> findAllByUserId(Long userId);
}
