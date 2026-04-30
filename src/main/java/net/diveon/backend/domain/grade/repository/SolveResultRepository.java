package net.diveon.backend.domain.grade.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import net.diveon.backend.domain.grade.entity.SovleResult;

public interface SolveResultRepository extends JpaRepository<SovleResult, Long>{
    
}
