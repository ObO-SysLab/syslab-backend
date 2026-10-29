package net.diveon.backend.domain.problem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import net.diveon.backend.domain.problem.service.ProblemOboService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<JsonNode> getOboJson(@AuthenticationPrincipal String userId, @RequestParam("id") Long problemId) {
        return ResponseEntity.ok(problemOboService.getOboJson(Long.parseLong(userId), problemId));
    }
}
