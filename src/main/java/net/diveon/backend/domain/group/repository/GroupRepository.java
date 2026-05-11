package net.diveon.backend.domain.group.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.group.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g WHERE " +
           "(:tag IS NULL OR EXISTS (SELECT gt FROM GroupTag gt WHERE gt.group = g AND gt.tag = :tag)) AND " +
           "(:userId IS NULL OR EXISTS (SELECT gu FROM GroupUser gu WHERE gu.group = g AND gu.user.id = :userId))")
    Page<Group> findAllWithFilters(@Param("tag") String tag, @Param("userId") Long userId, Pageable pageable);
}
