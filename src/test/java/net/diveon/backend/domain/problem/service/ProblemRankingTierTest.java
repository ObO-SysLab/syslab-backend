package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.entity.SolveSubmissionCoding;
import net.diveon.backend.domain.grade.repository.SolveSubmissionCodingRepository;
import net.diveon.backend.domain.grade.repository.SolveSubmissionRepository;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class ProblemRankingTierTest {
    private final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    private final SolveSubmissionCodingRepository coding = mock(SolveSubmissionCodingRepository.class);
    private final ProblemAccessService access = mock(ProblemAccessService.class);
    private final ProblemRankingService service = new ProblemRankingService(submissions, coding,
            mock(ProblemRepository.class), access);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private User user;

    private void fixture(String type, int page, Integer tier) {
        user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "nickname", "ranked-user");
        ReflectionTestUtils.setField(user, "tier", tier);
        var problem = new Problem(user, type, "fixture", "OS", "1", "public");
        ReflectionTestUtils.setField(problem, "id", 22L);
        var submission = new SolveSubmission(problem, user);
        ReflectionTestUtils.setField(submission, "id", 90L);
        when(access.requireRead(7L, 22L)).thenReturn(problem);
        when(submissions.findFirstCorrectByProblemId(22L, PageRequest.of(page - 1, 20)))
                .thenReturn(new PageImpl<>(List.of(submission), PageRequest.of(page - 1, 20), (page - 1) * 20 + 1));
        when(coding.findById(90L)).thenReturn(Optional.of(new SolveSubmissionCoding(submission, "source", "python")));
    }

    private JsonNode row(int page) {
        return mapper.valueToTree(service.getRanking(7L, 22L, page)).path("rankings").get(0);
    }

    @ParameterizedTest @ValueSource(strings = {"coding", "objective", "practice"})
    void serializesActualUserTierSeparatelyFromRankingPosition(String type) {
        fixture(type, 1, 8);
        JsonNode row = row(1);
        assertEquals(1, row.path("rank").intValue());
        assertTrue(row.path("userTier").isIntegralNumber());
        assertEquals(8, row.path("userTier").intValue());
        assertEquals(7L, row.path("userId").longValue());
        assertEquals("ranked-user", row.path("nickname").textValue());
        if ("coding".equals(type)) assertEquals("python", row.path("language").textValue());
        else { assertTrue(row.get("language").isNull()); verifyNoInteractions(coding); }
    }

    @Test void laterPageKeepsExistingRankOffsetAndCurrentStoredTier() {
        fixture("coding", 2, 8);
        assertEquals(21, row(2).path("rank").intValue());
        ReflectionTestUtils.setField(user, "tier", 12);
        JsonNode updated = row(2);
        assertEquals(21, updated.path("rank").intValue());
        assertEquals(12, updated.path("userTier").intValue());
    }

    @Test void missingStoredTierIsNullInsteadOfRankingPosition() {
        fixture("coding", 1, null);
        JsonNode row = row(1);
        assertTrue(row.has("userTier")); assertTrue(row.get("userTier").isNull());
        assertEquals(1, row.path("rank").intValue());
    }
}
