package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.response.OboTemplateResponse;
import net.diveon.backend.domain.problem.repository.OboTemplateRepository;
import org.springframework.stereotype.Service;

@Service
public class OboTemplateService {

    private final OboTemplateRepository oboTemplateRepository;

    public OboTemplateService(OboTemplateRepository oboTemplateRepository) {
        this.oboTemplateRepository = oboTemplateRepository;
    }

    public OboTemplateResponse getOboTemplates() {
        var templates = oboTemplateRepository.findAll();
        boolean enabled = !templates.isEmpty();

        return new OboTemplateResponse(
            200,
            "OBO 템플릿이 조회되었습니다.",
            enabled,
            templates
        );
    }
}
