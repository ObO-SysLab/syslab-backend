package net.diveon.backend.domain.group.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.group.entity.GroupAssignRequest;

public interface GroupAssignRequestRepository extends JpaRepository<GroupAssignRequest, Long> {
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, GroupAssignRequest.AssignRequestStatus status);

    // groupId와 userId를 함께 이용해 해당 유저가 해당 그룹에 남긴 가입 신청 기록 중 최신 1건을 조회한다.
    // 과거 CANCELED/REJECTED 이력이 있을 수 있으므로 appliedAt desc, id desc 기준으로 가장 최근 기록을 선택한다.
    // TODO: 동일 유저/그룹의 재신청 정책과 최신 신청 판별 기준에 대한 DB 관리 정책 변경이 필요하다.
    Optional<GroupAssignRequest> findFirstByGroupIdAndUserIdOrderByAppliedAtDescIdDesc(Long groupId, Long userId);
}
