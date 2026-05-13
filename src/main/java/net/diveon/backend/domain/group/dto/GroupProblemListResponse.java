package net.diveon.backend.domain.group.dto;

import net.diveon.backend.domain.group.entity.GroupProblem;

import java.util.List;

public class GroupProblemListResponse {

    private List<ProblemItem> problems;
    private int currentPage;
    private int totalPages;

    public GroupProblemListResponse(List<ProblemItem> problems, int currentPage, int totalPages) {
        this.problems = problems;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public List<ProblemItem> getProblems() { return problems; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }

    public static class ProblemItem {
        private Long problemId;
        private String title;
        private String author;
        private int solvedCount;
        private String difficulty;
        private String visibility;

        public ProblemItem(Long problemId, String title, String author, int solvedCount, String difficulty, String visibility) {
            this.problemId = problemId;
            this.title = title;
            this.author = author;
            this.solvedCount = solvedCount;
            this.difficulty = difficulty;
            this.visibility = visibility;
        }

        public static ProblemItem of(GroupProblem groupProblem) {
            return new ProblemItem(
                    groupProblem.getProblem().getId(),
                    groupProblem.getProblem().getTitle(),
                    groupProblem.getProblem().getAuthor().getNickname(),
                    groupProblem.getProblem().getSolvedCount(),
                    groupProblem.getProblem().getDifficulty(),
                    groupProblem.getProblem().getVisibility()
            );
        }

        public Long getProblemId() { return problemId; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public int getSolvedCount() { return solvedCount; }
        public String getDifficulty() { return difficulty; }
        public String getVisibility() { return visibility; }
    }
}
