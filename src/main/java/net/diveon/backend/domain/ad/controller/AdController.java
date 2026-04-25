package net.diveon.backend.domain.ad.controller;

import net.diveon.backend.domain.ad.dto.response.AdListResponse;
import net.diveon.backend.domain.ad.service.AdService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ad")
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdListResponse>> getAds(
        @RequestParam(value = "placement") String placement
    ) {
        try {
            AdListResponse responseData = adService.getAdsByPlacement(placement);
            return ResponseEntity.status(200)
                .body(ApiResponse.success("광고 조회에 성공하였습니다.", responseData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                .body(ApiResponse.success(e.getMessage(), null));
        }
    }
}
