package net.diveon.backend.domain.problem.controller;

import com.fasterxml.jackson.databind.JsonNode;
import net.diveon.backend.domain.problem.service.ProblemOboService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
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
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(problemOboService.getOboJson(Long.parseLong(userId), problemId));
    }

    @GetMapping("/obo/author")
    public ResponseEntity<JsonNode> getAuthorOboJson(@AuthenticationPrincipal String userId,
            @RequestParam("id") Long problemId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(problemOboService.getAuthorOboJson(Long.parseLong(userId), problemId));
    }

    @GetMapping("/obo/submission")
    public ResponseEntity<ProblemOboService.SubmissionObo> getSubmissionObo(@AuthenticationPrincipal String userId,
            @RequestParam("id") Long submissionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(problemOboService.getSubmissionObo(Long.parseLong(userId), submissionId));
    }

    @GetMapping("/obo/author/settings")
    public ResponseEntity<java.util.Map<String,Object>> getAuthorSettings(@AuthenticationPrincipal String userId,
            @RequestParam("id") Long problemId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(problemOboService.getAuthorSettings(Long.parseLong(userId),problemId));
    }

    @GetMapping("/obo/contest-submission")
    public ResponseEntity<ProblemOboService.ContestObo> getContestSubmissionObo(@AuthenticationPrincipal String userId,
            @RequestParam("id") Long submissionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(problemOboService.getContestSubmissionObo(Long.parseLong(userId),submissionId));
    }
}
