package net.diveon.backend.domain.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupUser;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {
}
