package net.diveon.backend.domain.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class MyGroupListResponse {

    private final Long total;
    private final Integer page;
    private final Integer pageSize;
    private final Integer totalPages;
    private final List<MyGroupItemResponse> groups;

    public MyGroupListResponse(Long total, Integer page, Integer pageSize, Integer totalPages, List<MyGroupItemResponse> groups) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.groups = groups;
    }

    public static MyGroupListResponse of(Page<?> pageResult, List<MyGroupItemResponse> groups) {
        return new MyGroupListResponse(
            pageResult.getTotalElements(),
            pageResult.getNumber() + 1,
            pageResult.getSize(),
            pageResult.getTotalPages(),
            groups
        );
    }

    public Long getTotal() { return total; }
    public Integer getPage() { return page; }
    public Integer getPageSize() { return pageSize; }
    public Integer getTotalPages() { return totalPages; }
    public List<MyGroupItemResponse> getGroups() { return groups; }
}
