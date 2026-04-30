package net.diveon.backend.domain.grade.dto.request.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 문제 요청의 공통 인터페이스
 * 
 * 문제의 요청은 무조건,
 * - 문제 번호
 * - 정답
 * 으로 구분한다.
 */
public interface SubmissionGradeRequest<T> {
    @JsonProperty("probId")
    long getProbId();

    /**
     * <pre>
     * 1. 제네릭 인터페이스 (가장 추천)
    인터페이스 자체를 제네릭으로 선언하여, 구현체에서 구체적인 타입을 지정하는 방식입니다.
    T는 String, String(TEXT), Int(or LIST<INT>)
    </pre>
     * @return T
     */
    @JsonProperty("answer")
    T getAnswer();
}
