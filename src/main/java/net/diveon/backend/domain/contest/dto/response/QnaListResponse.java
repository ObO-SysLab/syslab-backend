package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import net.diveon.backend.domain.contest.entity.ContestQna;

public class QnaListResponse {

    private final List<QnaItem> qnas;

    public QnaListResponse(List<QnaItem> qnas) {
        this.qnas = qnas;
    }

    public List<QnaItem> getQnas() { return qnas; }

    public static class QnaItem {
        private final Long qnaId;
        private final String question;
        private final String answer;
        private final boolean isAnswered;
        private final LocalDateTime createdAt;

        public QnaItem(Long qnaId, String question, String answer, boolean isAnswered, LocalDateTime createdAt) {
            this.qnaId = qnaId;
            this.question = question;
            this.answer = answer;
            this.isAnswered = isAnswered;
            this.createdAt = createdAt;
        }

        public static QnaItem of(ContestQna qna) {
            return new QnaItem(
                qna.getId(),
                qna.getQuestion(),
                qna.getAnswer(),
                qna.getAnswer() != null,
                qna.getCreatedAt()
            );
        }

        public Long getQnaId() { return qnaId; }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public boolean getIsAnswered() { return isAnswered; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
