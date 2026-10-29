package net.diveon.backend.domain.vm.service;

public interface DockerService {
    record Target(long sessionId, long userId, long problemId, long expiresAtEpochMs, String legacyContainerId) {}
    record State(String status, String containerId) {}
    String runContainer(String image, Target target);
    State inspect(Target target);
    void stopAndRemoveContainer(Target target);
    void reconcile();
}
