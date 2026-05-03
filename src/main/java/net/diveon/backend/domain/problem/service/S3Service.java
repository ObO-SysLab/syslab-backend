package net.diveon.backend.domain.problem.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    void uploadDockerfileZip(Long probId, MultipartFile file);
}
