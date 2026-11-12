package net.diveon.backend.domain.grade.service;

/** 채점 작업표에 등록된 코딩 제출. 커밋 뒤에 바로 발송하라는 신호로만 쓴다(놓쳐도 주기 작업이 보낸다). */
public record CodingGradeRequested(long submissionId) {}
