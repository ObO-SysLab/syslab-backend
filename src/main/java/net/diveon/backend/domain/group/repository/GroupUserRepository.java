package net.diveon.backend.domain.group.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.diveon.backend.domain.group.entity.GroupUser;

import java.util.List;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {
    long countByGroupId(Long groupId);
    Optional<GroupUser> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("""
            select gu
            from GroupUser gu
            join gu.user u
            where gu.group.id = :groupId
              and (:keyword is null or :keyword = '' or lower(u.nickname) like lower(concat('%', :keyword, '%')))
            order by
              case when gu.role = net.diveon.backend.domain.group.entity.GroupUser$GroupRole.LEADER then 0 else 1 end,
              gu.joinedAt asc,
              gu.id asc
            """)
    Page<GroupUser> findMembersByGroupIdAndKeyword(
            @Param("groupId") Long groupId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    List<GroupUser> findAllByUserId(Long userId);
    List<GroupUser> findAllByGroupIdIn(List<Long> groupIds);
}
