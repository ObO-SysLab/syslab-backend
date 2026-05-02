package net.diveon.backend.domain.vm.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("local")
public class DockerServiceMock implements DockerService {

    @Override
    public String runContainer(String image, Long userId, Long probId) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Override
    public void stopAndRemoveContainer(String containerId) {
    }
}
