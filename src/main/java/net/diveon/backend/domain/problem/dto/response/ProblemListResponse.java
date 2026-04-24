package net.diveon.backend.domain.problem.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class ProblemListResponse {

    private Long total;
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private List<ProblemListItemResponse> problems;

    public ProblemListResponse() {
    }

    public ProblemListResponse(Long total, Integer page, Integer pageSize, Integer totalPages, List<ProblemListItemResponse> problems) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.problems = problems;
    }

    public static ProblemListResponse of(Page<?> page, List<ProblemListItemResponse> problems) {
        return new ProblemListResponse(
            page.getTotalElements(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalPages(),
            problems
        );
    }

    public Long getTotal() {
        return total;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public List<ProblemListItemResponse> getProblems() {
        return problems;
    }
}
