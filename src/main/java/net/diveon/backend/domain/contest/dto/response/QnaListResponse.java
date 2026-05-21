package net.diveon.backend.domain.contest.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import net.diveon.backend.domain.contest.entity.ContestQna;
import net.diveon.backend.domain.contest.entity.ContestQnaAnswer;

public class QnaListResponse {

    private final List<QnaItem> qnas;

    public QnaListResponse(List<QnaItem> qnas) {
        this.qnas = qnas;
    }

    public List<QnaItem> getQnas() { return qnas; }

    public static class QnaItem {
        private final Long qnaId;
        private final String question;
        private final List<AnswerItem> answers;
        private final boolean isAnswered;
        private final LocalDateTime createdAt;

        public QnaItem(Long qnaId, String question, List<AnswerItem> answers, boolean isAnswered, LocalDateTime createdAt) {
            this.qnaId = qnaId;
            this.question = question;
            this.answers = answers;
            this.isAnswered = isAnswered;
            this.createdAt = createdAt;
        }

        public static QnaItem of(ContestQna qna) {
            List<AnswerItem> answers = qna.getAnswers().stream().map(AnswerItem::of).toList();
            return new QnaItem(
                qna.getId(),
                qna.getQuestion(),
                answers,
                !answers.isEmpty(),
                qna.getCreatedAt()
            );
        }

        public Long getQnaId() { return qnaId; }
        public String getQuestion() { return question; }
        public List<AnswerItem> getAnswers() { return answers; }
        public boolean getIsAnswered() { return isAnswered; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class AnswerItem {
        private final Long answerId;
        private final String answer;
        private final LocalDateTime answeredAt;

        public AnswerItem(Long answerId, String answer, LocalDateTime answeredAt) {
            this.answerId = answerId;
            this.answer = answer;
            this.answeredAt = answeredAt;
        }

        public static AnswerItem of(ContestQnaAnswer qnaAnswer) {
            return new AnswerItem(qnaAnswer.getId(), qnaAnswer.getAnswer(), qnaAnswer.getAnsweredAt());
        }

        public Long getAnswerId() { return answerId; }
        public String getAnswer() { return answer; }
        public LocalDateTime getAnsweredAt() { return answeredAt; }
    }
}
