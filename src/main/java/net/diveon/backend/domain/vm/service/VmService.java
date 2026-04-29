package net.diveon.backend.domain.vm.service;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.vm.dto.request.VmCreateRequest;
import net.diveon.backend.domain.vm.dto.response.VmCreateResponse;
import net.diveon.backend.domain.vm.dto.response.VmDeleteResponse;
import net.diveon.backend.domain.vm.entity.VmContainer;
import net.diveon.backend.domain.vm.repository.VmContainerRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import net.diveon.backend.global.exception.VmAccessDeniedException;
import net.diveon.backend.global.exception.VmAlreadyExistsException;
import net.diveon.backend.global.exception.VmNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VmService {

    private final VmContainerRepository vmContainerRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final DockerService dockerService;

    public VmService(VmContainerRepository vmContainerRepository,
                     ProblemPracticeRepository problemPracticeRepository,
                     DockerService dockerService) {
        this.vmContainerRepository = vmContainerRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.dockerService = dockerService;
    }

    @Transactional
    public VmCreateResponse createVm(Long userId, VmCreateRequest request) {
        Long probId = request.getProbId();

        ProblemPractice practice = problemPracticeRepository.findById(probId)
                .orElseThrow(() -> new ProblemNotFoundException("존재하지 않는 실습 문제입니다."));

        if (vmContainerRepository.existsByUserId(userId)) {
            throw new VmAlreadyExistsException("이미 실행 중인 VM이 있습니다.");
        }

        String image = practice.getEcrImageUri();
        String containerId = dockerService.createContainer(image);

        LocalDateTime now = LocalDateTime.now();
        VmContainer vm = new VmContainer(userId, containerId, probId, image, now, now.plusHours(2));
        vmContainerRepository.save(vm);

        return VmCreateResponse.from(vm);
    }

    @Transactional
    public VmDeleteResponse deleteVm(Long userId, String containerId) {
        VmContainer vm = vmContainerRepository.findByContainerId(containerId)
                .orElseThrow(() -> new VmNotFoundException("존재하지 않는 컨테이너입니다."));

        if (!vm.getUserId().equals(userId)) {
            throw new VmAccessDeniedException("본인의 VM만 삭제할 수 있습니다.");
        }

        dockerService.removeContainer(containerId);
        vmContainerRepository.delete(vm);

        return new VmDeleteResponse(containerId, LocalDateTime.now());
    }
}