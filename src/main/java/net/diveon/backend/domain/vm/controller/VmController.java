package net.diveon.backend.domain.vm.controller;

import jakarta.validation.Valid;
import net.diveon.backend.domain.vm.dto.request.VmCreateRequest;
import net.diveon.backend.domain.vm.dto.response.VmCreateResponse;
import net.diveon.backend.domain.vm.service.VmService;
import net.diveon.backend.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vm")
public class VmController {

    private final VmService vmService;

    public VmController(VmService vmService) {
        this.vmService = vmService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VmCreateResponse>> createVm(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody VmCreateRequest request
    ) {
        VmCreateResponse response = vmService.createVm(Long.parseLong(userId), request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("VM 환경이 성공적으로 생성되었습니다.", response));
    }

    @DeleteMapping("/{container_id}")
    public ResponseEntity<ApiResponse<Void>> deleteVm(
            @AuthenticationPrincipal String userId,
            @PathVariable("container_id") String containerId
    ) {
        vmService.deleteVm(Long.parseLong(userId), containerId);
        return ResponseEntity.ok()
                .body(ApiResponse.success("VM이 성공적으로 삭제되었습니다.", null));
    }
}