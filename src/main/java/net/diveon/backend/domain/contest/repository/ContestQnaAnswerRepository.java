package net.diveon.backend.domain.contest.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.contest.entity.ContestQnaAnswer;

public interface ContestQnaAnswerRepository extends JpaRepository<ContestQnaAnswer, Long> {
    Optional<ContestQnaAnswer> findByIdAndQnaId(Long id, Long qnaId);
}
