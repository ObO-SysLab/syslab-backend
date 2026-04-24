package net.diveon.backend.domain.problem.service;

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

@Service
public class ProblemListService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private static final int PAGE_SIZE = 10;

    public ProblemListService(UserRepository userRepository, ProblemRepository problemRepository) {
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public ProblemListResponse listProblems(
        long userId,
        int page,
        String title,
        String category,
        String difficulty
    ) {
        userRepository.findById(userId).orElseThrow();

        Pageable pageable = PageRequest.of(
            page - 1,
            PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<Problem> spec = buildSpecification(title, category, difficulty);
        Page<Problem> problemPage = problemRepository.findAll(spec, pageable);

        List<ProblemListItemResponse> problemList = problemPage.getContent()
            .stream()
            .map(ProblemListItemResponse::of)
            .toList();

        return ProblemListResponse.of(problemPage, problemList);
    }

    private Specification<Problem> buildSpecification(String title, String category, String difficulty) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<>();

            if (title != null && !title.isEmpty()) {
                predicates.add(cb.like(root.get("title"), "%" + title + "%"));
            }

            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (difficulty != null && !difficulty.isEmpty()) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
    }
}
