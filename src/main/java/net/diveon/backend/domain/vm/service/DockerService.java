package net.diveon.backend.domain.vm.service;

public interface DockerService {

    String runContainer(String image, Long userId, Long probId);

    void stopAndRemoveContainer(String containerId);
}
