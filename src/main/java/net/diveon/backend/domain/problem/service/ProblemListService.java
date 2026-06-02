package net.diveon.backend.domain.problem.service;

import jakarta.persistence.criteria.Predicate;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.dto.response.ProblemListItemResponse;
import net.diveon.backend.domain.problem.dto.response.ProblemListResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ProblemListService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final SolveSubmissionRepository solveSubmissionRepository;
    private static final int PAGE_SIZE = 10;

    public ProblemListService(UserRepository userRepository, ProblemRepository problemRepository,
                              SolveSubmissionRepository solveSubmissionRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.solveSubmissionRepository = solveSubmissionRepository;
    }

    @Transactional(readOnly = true)
    public ProblemListResponse listProblems(
        long userId,
        int page,
        String title,
        String type,
        String category,
        String difficulty,
        boolean onlyUnsolved,
        boolean onlyMine
    ) {
        userRepository.findById(userId).orElseThrow();

        Set<Long> solvedIds = Set.copyOf(solveSubmissionRepository.findSolvedProblemIdsByUserId(userId));

        Pageable pageable = PageRequest.of(
            page - 1,
            PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<Problem> spec = buildSpecification(userId, title, type, category, difficulty, onlyUnsolved, onlyMine, solvedIds);
        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);

        List<ProblemListItemResponse> problemList = problemPage.getContent()
            .stream()
            .map(problem -> ProblemListItemResponse.of(problem, solvedIds.contains(problem.getId())))
            .toList();

        return ProblemListResponse.of(problemPage, problemList);
    }

    private Specification<Problem> buildSpecification(long userId, String title, String type, String category, String difficulty,
                                                      boolean onlyUnsolved, boolean onlyMine, Set<Long> solvedIds) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<>();

            if (onlyMine) {
                predicates.add(cb.equal(root.get("author").get("id"), userId));
                predicates.add(cb.equal(root.get("visibility"), "private"));
            } else {
                predicates.add(cb.equal(root.get("visibility"), "public"));
            }

            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title + "%"));
            }

            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (difficulty != null && !difficulty.isEmpty()) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (onlyUnsolved && !solvedIds.isEmpty()) {
                predicates.add(root.get("id").in(solvedIds).not());
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
