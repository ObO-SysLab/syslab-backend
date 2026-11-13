package net.diveon.backend.domain.problem.service;

import java.util.List;

public interface EcrService {
    /** commandPolicy: the build's normalized allowed commands (its marker tag is removed too), or null. */
    void deleteImage(Long probId, String buildId, List<String> commandPolicy);
}
