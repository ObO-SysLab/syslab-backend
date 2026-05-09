package net.diveon.backend.domain.vm.service;

import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.vm.dto.request.VmCreateRequest;
import net.diveon.backend.domain.vm.dto.request.VmStopRequest;
import net.diveon.backend.domain.vm.dto.response.VmCreateResponse;
import net.diveon.backend.domain.vm.dto.response.VmStatusResponse;
import net.diveon.backend.domain.vm.dto.response.VmStopResponse;
import net.diveon.backend.domain.vm.entity.VmSession;
import net.diveon.backend.domain.vm.repository.VmSessionRepository;
import net.diveon.backend.global.exception.ImageNotReadyException;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import net.diveon.backend.global.exception.VmNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VmService {

    private final VmSessionRepository vmSessionRepository;
    private final ProblemPracticeRepository problemPracticeRepository;
    private final DockerService dockerService;

    public VmService(VmSessionRepository vmSessionRepository,
                     ProblemPracticeRepository problemPracticeRepository,
                     DockerService dockerService) {
        this.vmSessionRepository = vmSessionRepository;
        this.problemPracticeRepository = problemPracticeRepository;
        this.dockerService = dockerService;
    }

    // VM 상태 조회
    @Transactional(readOnly = true)
    public VmStatusResponse getStatus(Long userId, Long probId) {
        ProblemPractice practice = problemPracticeRepository.findById(probId)
                .orElseThrow(() -> new ProblemNotFoundException("존재하지 않는 실습 문제입니다."));

        if (!"READY".equals(practice.getImageStatus())) {
            return VmStatusResponse.imageNotReady();
        }

        Optional<VmSession> session = vmSessionRepository.findFirstByUserIdAndStatus(userId, "RUNNING");
        if (session.isPresent()) {
            return VmStatusResponse.running(session.get().getContainerId());
        }

        return VmStatusResponse.none();
    }

    // VM 생성
    @Transactional
    public VmCreateResponse createVm(Long userId, VmCreateRequest request) {
        Long probId = request.getProbId();

        ProblemPractice practice = problemPracticeRepository.findById(probId)
                .orElseThrow(() -> new ProblemNotFoundException("존재하지 않는 실습 문제입니다."));
        
        // image_status = READY 확인
        if (!"READY".equals(practice.getImageStatus())) {
            throw new ImageNotReadyException("이미지가 아직 준비 중입니다. (image_status: " + practice.getImageStatus() + ")");
        }

        // 이미 기존 생성된 VM이 있는지 확인, 있으면 반환 (유저당 VM 생성 1개 제한)
        Optional<VmSession> existing = vmSessionRepository.findFirstByUserIdAndStatus(userId, "RUNNING");
        if (existing.isPresent()) {
            return VmCreateResponse.from(existing.get(), "기존 VM을 반환합니다.");
        }

        // 없으면 DockerService 호출해서 컨테이너 생성하고, DB에 세션 저장하고, 응답 반환
        String containerId = dockerService.runContainer(practice.getEcrImageUri(), userId, probId);

        LocalDateTime now = LocalDateTime.now();
        VmSession session = new VmSession(userId, probId, containerId, now, now.plusHours(2)); // now = 생성시각, now.plusHour(2) = 2시간 후 만료 시각
        vmSessionRepository.save(session);

        return VmCreateResponse.from(session, "VM이 생성되었습니다.");
    }

    // VM 중지
    @Transactional
    public VmStopResponse stopVm(Long userId, VmStopRequest request) {
        VmSession session = vmSessionRepository.findFirstByUserIdAndStatus(userId, "RUNNING")
                .orElseThrow(() -> new VmNotFoundException("실행 중인 VM이 없습니다."));

        dockerService.stopAndRemoveContainer(session.getContainerId()); // 컨테이너 중단 & 삭제
        session.stop(); // DB에서 status = "STOPPED"로 변경

        return new VmStopResponse("success", "VM이 종료되었습니다."); 
    }

    // VM 리셋
    @Transactional
    public VmCreateResponse resetVm(Long userId, VmStopRequest request) {
        stopVm(userId, request);
        VmCreateRequest createRequest = new VmCreateRequest(); // 빈 객체 만들고
        createRequest.setProbId(request.getProbId()); // prob_id만 복사
        return createVm(userId, createRequest); // createVm에 넘김
    }// 즉, VmStopRequest에서 prob_id만 꺼내서 VmCreateRequest에 담아줌
}
