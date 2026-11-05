package net.diveon.backend.domain.grade.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.grade.entity.SolveSubmission;
import net.diveon.backend.domain.grade.repository.*;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.service.ProblemAccessService;
import net.diveon.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

class GradeBoardMetadataTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SolveSubmissionRepository submissions = mock(SolveSubmissionRepository.class);
    private final ProblemCodingRepository coding = mock(ProblemCodingRepository.class);
    private final ProblemObjectiveRepository objective = mock(ProblemObjectiveRepository.class);
    private final ProblemAccessService access = mock(ProblemAccessService.class);
    private final GradeBoardService service = new GradeBoardService(submissions,
            mock(SolveSubmissionCodingRepository.class), mock(SolveResultRepository.class),
            mock(SolveResultCodingRepository.class), mock(ProblemRepository.class), access,
            new SubmissionOboPresence(coding, objective));
    private User user;
    private Problem problem;
    private SolveSubmission submission;

    private void fixture(String type, String snapshot) throws Exception {
        user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        ReflectionTestUtils.setField(user, "nickname", "submitter");
        ReflectionTestUtils.setField(user, "tier", 8);
        problem = new Problem(user, type, "fixture", "OS", "easy", "public");
        ReflectionTestUtils.setField(problem, "id", 22L);
        submission = new SolveSubmission(problem, user);
        if (snapshot != null) submission.bindRevision(new ProblemRevision("saved-revision", 22L, mapper.readTree(snapshot)));
        ReflectionTestUtils.setField(submission, "id", 90L);
        submission.setSubmissionState(SolveSubmission.SubmissionState.COMPLETED);
        when(access.requireRead(7L, 22L)).thenReturn(problem);
        when(submissions.findTerminalByProbId(eq(22L), any())).thenReturn(new PageImpl<>(List.of(submission)));
        when(submissions.findCompletedByProbId(eq(22L), any())).thenReturn(new PageImpl<>(List.of(submission)));
        when(submissions.countCompletedByProbId(22L)).thenReturn(1L);
    }

    private JsonNode row() {
        return mapper.valueToTree(service.getBoard(7L, 22L, 1, 20, "all", "latest")).path("submissions").get(0);
    }

    @ParameterizedTest @ValueSource(strings = {"coding", "objective", "practice"})
    void boardSerializesStoredUserTierAndNoOboForPlainProblem(String type) throws Exception {
        fixture(type, "{\"obo\":null}");
        JsonNode row = row();
        assertTrue(row.path("userTier").isIntegralNumber());
        assertEquals(8, row.path("userTier").intValue());
        assertTrue(row.path("hasObo").isBoolean()); assertFalse(row.path("hasObo").booleanValue());
        assertEquals(90L, row.path("submissionId").longValue());
        verifyNoInteractions(coding, objective);
    }

    @Test void missingTierRemainsNullInsteadOfInventingBronze() throws Exception {
        fixture("coding", "{\"obo\":null}");
        ReflectionTestUtils.setField(user, "tier", null);
        assertTrue(row().has("userTier")); assertTrue(row().get("userTier").isNull());
    }

    @ParameterizedTest @ValueSource(strings = {"coding", "objective"})
    void historicalOboSurvivesRemovalWithoutExposingDocument(String type) throws Exception {
        fixture(type, "{\"obo\":{\"mode\":\"coding_diff\",\"privateLabel\":\"answer diagram\"}}");
        JsonNode row = row();
        assertTrue(row.path("hasObo").booleanValue());
        assertFalse(row.has("obo")); assertFalse(row.toString().contains("answer diagram"));
        verifyNoInteractions(coding, objective);
    }

    @ParameterizedTest @ValueSource(strings = {"{\"obo\":null}", "{}", "{\"obo\":{}}"})
    void historicalPlainCodingDoesNotInheritNewObo(String snapshot) throws Exception {
        fixture("coding", snapshot);
        var current = new ProblemCoding(); current.setOboJsonData(mapper.readTree("{\"mode\":\"coding_diff\"}"));
        when(coding.findById(22L)).thenReturn(Optional.of(current));
        assertFalse(row().path("hasObo").booleanValue());
        verifyNoInteractions(coding, objective);
    }

    @ParameterizedTest @ValueSource(strings = {"coding", "objective"})
    void revisionlessLegacyUsesCurrentDocument(String type) throws Exception {
        fixture(type, null);
        JsonNode document = mapper.readTree("{\"mode\":\"legacy\"}");
        if ("coding".equals(type)) {
            var current = new ProblemCoding(); current.setOboJsonData(document);
            when(coding.findById(22L)).thenReturn(Optional.of(current));
        } else {
            var current = new ProblemObjective(); current.setOboJsonData(document);
            when(objective.findById(22L)).thenReturn(Optional.of(current));
        }
        assertTrue(row().path("hasObo").booleanValue());
    }

    @Test void revisionlessCodingWithoutCurrentOboIsFalse() throws Exception {
        fixture("coding", null);
        when(coding.findById(22L)).thenReturn(Optional.of(new ProblemCoding()));
        assertFalse(row().path("hasObo").booleanValue());
    }
}
