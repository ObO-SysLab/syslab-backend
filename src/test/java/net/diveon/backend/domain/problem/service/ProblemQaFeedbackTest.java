package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import net.diveon.backend.domain.problem.dto.request.ProblemUpdateObjectiveRequest;
import net.diveon.backend.domain.problem.dto.response.ProblemCommentResponse;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemComment;
import net.diveon.backend.domain.problem.entity.ProblemObjective;
import net.diveon.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;

/** 2026-11-10 user QA: ordered objective answers survive editing; comments carry the author picture. */
class ProblemQaFeedbackTest {
    private static ProblemObjective objective(boolean ordered) {
        return new ProblemObjective(new Problem(), "s", "d", List.of(), List.of(3, 1), ordered, false);
    }

    @Test void updateRequestReadsTheOrderedOptionUnderTheApiName() throws Exception {
        var request = new ObjectMapper().readValue("{\"isSeqeuntialAnswer\":true,\"answer\":[3,1]}", ProblemUpdateObjectiveRequest.class);
        assertEquals(Boolean.TRUE, request.getIsSeqeuntialAnswer());
        assertEquals(List.of(3, 1), request.getAnswer());
        assertNull(new ObjectMapper().readValue("{}", ProblemUpdateObjectiveRequest.class).getIsSeqeuntialAnswer());
    }

    @Test void editingTurnsTheOrderedOptionOnAndOffAndKeepsItWhenAbsent() {
        var problem = objective(false);
        problem.updateAssessment(null, List.of(3, 1), true, false);
        assertTrue(problem.getIsSeqeuntialAnswer());
        assertEquals(List.of(3, 1), problem.getAnswer(), "the pick order is the answer order");
        problem.updateAssessment(null, null, null, false);
        assertTrue(problem.getIsSeqeuntialAnswer(), "older clients that do not send the option keep it");
        problem.updateAssessment(null, null, false, false);
        assertFalse(problem.getIsSeqeuntialAnswer());
    }

    @Test void simulationProblemsAreNeverOrdered() {
        var problem = objective(true);
        problem.updateAssessment(null, null, true, true);
        assertFalse(problem.getIsSeqeuntialAnswer());
    }

    @Test void commentsListDetailAndRepliesCarryTheAuthorPicture() throws Exception {
        var author = User.createSocialUser("login", "pw", "다이버", "a@b.c", "실명", "https://cdn.example/profiles/users/7");
        var plain = new User("login2", "pw", "무사진", "b@c.d", null, List.of());
        var comment = new ProblemComment(new Problem(), author, null, "질문", false);
        var reply = new ProblemComment(new Problem(), plain, comment, "답", false);
        var mapper = new ObjectMapper();

        var listed = mapper.readTree(mapper.writeValueAsString(ProblemCommentResponse.CommentListItem.of(comment)));
        assertEquals("https://cdn.example/profiles/users/7", listed.path("authorProfileImgUrl").asText());
        var replyItem = ProblemCommentResponse.ReplyItem.of(reply);
        var detail = mapper.readTree(mapper.writeValueAsString(ProblemCommentResponse.CommentItem.of(comment, List.of(replyItem))));
        assertEquals("https://cdn.example/profiles/users/7", detail.path("authorProfileImgUrl").asText());
        assertTrue(detail.path("replies").path(0).has("authorProfileImgUrl"));
        assertTrue(detail.path("replies").path(0).path("authorProfileImgUrl").isNull(), "no picture is null, not a made-up URL");
        assertEquals("다이버", listed.path("authorNickname").asText());
    }
}
