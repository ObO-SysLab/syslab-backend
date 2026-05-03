package net.diveon.backend.domain.grade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;

public interface SolveSubmissionCodingRepository extends JpaRepository<SolveSubmissionCoding, Long>{
    
}
