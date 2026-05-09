package net.diveon.backend.domain.grade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SolveResultCoding;

public interface SolveResultCodingRepository extends JpaRepository<SolveResultCoding, Long> {
}
