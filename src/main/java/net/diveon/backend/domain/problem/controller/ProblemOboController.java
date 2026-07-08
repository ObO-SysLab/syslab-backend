package net.diveon.backend.domain.problem.controller;

import net.diveon.backend.domain.problem.dto.response.OboPassThroughResponse;
import net.diveon.backend.domain.problem.service.ProblemOboService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
public class ProblemOboController {

    private final ProblemOboService problemOboService;

    public ProblemOboController(ProblemOboService problemOboService) {
        this.problemOboService = problemOboService;
    }

    @GetMapping("/obo")
    public ResponseEntity<OboPassThroughResponse> getOboJson(@RequestParam("id") Long problemId) {
        return ResponseEntity.ok(problemOboService.getOboJson(problemId));
    }
}
