package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3Service {
    /** commandPolicy: normalized allowed commands stored as object metadata, or null for unrestricted. */
    void uploadDockerfileZip(Long probId, String requestId, MultipartFile file, List<String> commandPolicy);

    /** Whether the request-scoped source of an earlier build is still stored. */
    boolean hasDockerfileZip(Long probId, String requestId);

    /** Rebuild from an earlier source under a new request, with the new policy as metadata (starts a build). */
    void copyDockerfileZip(Long probId, String fromRequestId, String toRequestId, List<String> commandPolicy);
    void uploadCodingTestcases(Long probId, List<ForDtoTestCase> testcases);

    void deleteDockerfileZip(Long probId, String requestId);
    void deleteCodingTestcases(Long probId);
}
