package net.diveon.backend.domain.vm.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.vm.dto.request.VmCreateRequest;
import net.diveon.backend.domain.vm.dto.request.VmStopRequest;
import net.diveon.backend.domain.vm.dto.response.VmCreateResponse;
import net.diveon.backend.domain.vm.dto.response.VmStatusResponse;
import net.diveon.backend.domain.vm.dto.response.VmStopResponse;
import net.diveon.backend.domain.vm.service.VmService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


// 실습형 VM 환경 상태 조회/생성/종료/초기화 API
@RestController
@RequestMapping("/api/vm")
public class VmController {

    private final VmService vmService;

    public VmController(VmService vmService) {
        this.vmService = vmService;
    }

    // VM 상태 조회
    @GetMapping("/status/{probId}")
    public ResponseEntity<ApiResponse<VmStatusResponse>> getStatus(
            @AuthenticationPrincipal String userId,
            @PathVariable Long probId
    ) {
        VmStatusResponse response = vmService.getStatus(Long.parseLong(userId), probId);
        return ResponseEntity.ok(ApiResponse.success("VM 상태를 조회했습니다.", response));
    }

    // VM 생성
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<VmCreateResponse>> createVm(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody VmCreateRequest request
    ) {
        VmCreateResponse response = vmService.createVm(Long.parseLong(userId), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    // VM 중지
    @DeleteMapping("/stop")
    public ResponseEntity<ApiResponse<VmStopResponse>> stopVm(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody VmStopRequest request
    ) {
        VmStopResponse response = vmService.stopVm(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    // VM 리셋
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<VmCreateResponse>> resetVm(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody VmStopRequest request
    ) {
        VmCreateResponse response = vmService.resetVm(Long.parseLong(userId), request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
