package net.diveon.backend.domain.vm.service;

public interface DockerService {

    String createContainer(String image);

    void removeContainer(String containerId);
}