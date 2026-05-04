package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3Service {
    void uploadDockerfileZip(Long probId, MultipartFile file);

    void uploadCodingTestcases(Long probId, List<ForDtoTestCase> testcases);
}
