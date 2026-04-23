package net.diveon.backend.domain.problem.dto.response.interfaces;

/**
 * 문제 상세 조회 응답 DTO들이 공통으로 구현해야 하는 인터페이스입니다.
 *
 * <p>
 * 현재 문제 상세 조회는 objective, practice, coding 등 문제 유형별로
 * 서로 다른 응답 DTO를 가질 수 있습니다.
 * 하지만 모든 문제 유형은 Problem 엔티티의 공통 컬럼을 기반으로 하는
 * 기본 문제 정보를 응답에 포함해야 합니다.
 * </p>
 *
 * <p>
 * 따라서 이 인터페이스에 공통 getter를 명시하여,
 * 컨트롤러와 서비스가 특정 문제 유형 DTO에 직접 의존하지 않고
 * ProblemDetailResponse라는 공통 타입으로 응답을 다룰 수 있도록 합니다.
 * </p>
 */
public interface ProblemDetailResponse {

    /**
     * 문제 상세 조회의 기준이 되는 Problem 엔티티의 id입니다.
     * 응답 JSON에서는 prob_id로 내려갑니다.
     */
    Long getProbId();

    /**
     * 문제 작성자 정보입니다.
     * Problem 엔티티는 User 엔티티를 author로 참조하지만,
     * 응답에서는 User 전체가 아니라 nickname 문자열만 제공합니다.
     */
    String getAuthor();

    /**
     * 문제 유형입니다.
     * objective, practice, coding 등 상세 응답 분기 기준으로 사용됩니다.
     */
    String getType();

    /**
     * 문제 제목입니다.
     * 모든 문제 유형에서 공통적으로 제공되는 핵심 표시 정보입니다.
     */
    String getTitle();

    /**
     * 문제 카테고리입니다.
     * 문제 목록, 상세 화면, 필터링 기준에서 공통적으로 사용할 수 있는 정보입니다.
     */
    String getCategory();

    /**
     * 문제 난이도입니다.
     * easy, medium, hard 등 모든 문제 유형에서 공통으로 표시될 수 있습니다.
     */
    String getDifficulty();

    /**
     * 문제 공개 범위입니다.
     * public, private, group 등 접근 정책이나 화면 표시 판단에 사용할 수 있습니다.
     */
    String getVisibility();

    /**
     * 문제를 성공적으로 해결한 횟수입니다.
     * 모든 문제 유형에서 통계 정보로 공통 제공될 수 있습니다.
     */
    Integer getSolvedCount();

    /**
     * 문제 제출 횟수입니다.
     * 풀이 시도 수 또는 제출 통계로 모든 문제 유형에서 공통 제공될 수 있습니다.
     */
    Integer getSubmittedCount();

    /**
     * 문제 생성 시각입니다.
     * 모든 문제 유형에서 기본 메타데이터로 제공됩니다.
     */
    String getCreatedAt();

    /**
     * 문제 최종 수정 시각입니다.
     * 모든 문제 유형에서 기본 메타데이터로 제공됩니다.
     */
    String getUpdatedAt();
}
