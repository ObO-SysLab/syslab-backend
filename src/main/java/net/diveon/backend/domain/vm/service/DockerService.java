package net.diveon.backend.domain.vm.service;

public interface DockerService {

    String createContainer(Long probId);

    void removeContainer(String containerId);
}