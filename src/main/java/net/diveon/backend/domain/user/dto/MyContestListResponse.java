package net.diveon.backend.domain.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class MyContestListResponse {

    private final Long total;
    private final Integer page;
    private final Integer pageSize;
    private final Integer totalPages;
    private final List<MyContestItemResponse> contests;

    public MyContestListResponse(Long total, Integer page, Integer pageSize, Integer totalPages, List<MyContestItemResponse> contests) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.contests = contests;
    }

    public static MyContestListResponse of(Page<?> pageResult, List<MyContestItemResponse> contests) {
        return new MyContestListResponse(
            pageResult.getTotalElements(),
            pageResult.getNumber() + 1,
            pageResult.getSize(),
            pageResult.getTotalPages(),
            contests
        );
    }

    public Long getTotal() { return total; }
    public Integer getPage() { return page; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotalPages() { return totalPages; }
    public List<MyContestItemResponse> getContests() { return contests; }
}
