package net.diveon.backend.domain.problem.controller;

import net.diveon.backend.domain.problem.dto.response.OboTemplateResponse;
import net.diveon.backend.domain.problem.service.OboTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems/obo")
public class OboTemplateController {

    private final OboTemplateService oboTemplateService;

    public OboTemplateController(OboTemplateService oboTemplateService) {
        this.oboTemplateService = oboTemplateService;
    }

    @GetMapping
    public ResponseEntity<OboTemplateResponse> getOboTemplates() {
        System.out.println("=============== OBO API CALLED ===============");
        OboTemplateResponse responseData = oboTemplateService.getOboTemplates();
        System.out.println("=============== OBO API SUCCESS: " + responseData.getStatus() + " ===============");
        return ResponseEntity.status(200).body(responseData);
    }
}
