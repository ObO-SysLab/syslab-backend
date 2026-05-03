package net.diveon.backend.domain.ad.service;

import net.diveon.backend.domain.ad.dto.response.AdListResponse;
import net.diveon.backend.domain.ad.dto.response.AdResponse;
import net.diveon.backend.domain.ad.entity.Ad;
import net.diveon.backend.domain.ad.repository.AdRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdService {

    private final AdRepository adRepository;

    // Placement별 광고 규격 (width, height)
    private static final Map<String, int[]> PLACEMENT_DIMENSIONS = Map.of(
        "main", new int[]{1200, 250},
        "probList", new int[]{1200, 250},
        "probDetail", new int[]{300, 600}
    );

    public AdService(AdRepository adRepository) {
        this.adRepository = adRepository;
    }

    public AdListResponse getAdsByPlacement(String placement) {
        if (!PLACEMENT_DIMENSIONS.containsKey(placement)) {
            throw new IllegalArgumentException("유효하지 않은 placement 값입니다. 허용값: main, probList, probDetail");
        }

        List<Ad> ads = adRepository.findByPlacementAndExpiresAtAfter(placement, LocalDateTime.now());
        List<AdResponse> adResponses = ads.stream()
            .map(AdResponse::new)
            .collect(Collectors.toList());

        int[] dimensions = PLACEMENT_DIMENSIONS.get(placement);
        return new AdListResponse(placement, dimensions[0], dimensions[1], adResponses);
    }
}
