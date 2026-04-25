package net.diveon.backend.domain.ad.repository;

import net.diveon.backend.domain.ad.entity.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long> {
    List<Ad> findByPlacementAndExpiresAtAfter(String placement, LocalDateTime now);
}
