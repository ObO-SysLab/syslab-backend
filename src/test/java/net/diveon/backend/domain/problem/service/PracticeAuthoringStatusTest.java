package net.diveon.backend.domain.problem.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import net.diveon.backend.domain.contest.repository.*;
import net.diveon.backend.domain.group.repository.*;
import net.diveon.backend.domain.problem.dto.response.ProblemDetailPracticeResponse;
import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.global.exception.ProblemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PracticeAuthoringStatusTest {
    final ProblemRepository problems = mock(ProblemRepository.class);
    final ProblemPracticeRepository practices = mock(ProblemPracticeRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final GroupProblemRepository groupProblems = mock(GroupProblemRepository.class);
    final ProblemAccessService access = new ProblemAccessService(problems, groupProblems,
            mock(GroupUserRepository.class), mock(ContestProblemRepository.class), mock(ContestParticipantRepository.class));
    final ProblemDetailService details = new ProblemDetailService(users, problems,
            mock(ProblemObjectiveRepository.class), practices, mock(ProblemCodingRepository.class),
            mock(OboStepRepository.class), access, mock(OboDisclosureService.class));
    Problem problem;
    ProblemPractice practice;

    @BeforeEach void fixture() {
        var owner = new User("author", "unused", "author", "author@example.test", null, null);
        ReflectionTestUtils.setField(owner, "id", 1L);
        problem = new Problem(owner, "practice", "Authoring status", "kernel", "1", "private");
        ReflectionTestUtils.setField(problem, "id", 9L);
        practice = new ProblemPractice(problem, "summary", "description",
                "private-flag-hash", "s3://private-source/source.zip");
        ReflectionTestUtils.setField(practice, "codeBuildId", "private-project:internal-build-id");
        ReflectionTestUtils.setField(practice, "buildRequestId", "internal-request-id");
        when(problems.findById(9L)).thenReturn(Optional.of(problem));
        when(practices.findById(9L)).thenReturn(Optional.of(practice));
        when(users.findById(1L)).thenReturn(Optional.of(owner));
        when(users.findById(2L)).thenReturn(Optional.of(new User()));
    }

    ProblemDetailPracticeResponse detail(long userId) {
        return (ProblemDetailPracticeResponse) details.detailProblem(userId, 9);
    }

    @Test void authorCanReopenPendingPrivateProblemWithoutStartingExecution() {
        var response = detail(1);
        assertThat(response.getIsOwner()).isTrue();
        assertThat(response.getPracticeBuild().status()).isEqualTo("PENDING");
        assertThat(response.getPracticeBuild().canStart()).isFalse();
        verify(practices, never()).save(any());
    }

    @ParameterizedTest @ValueSource(strings = {"PENDING", "FAILED", "READY"})
    void anotherUserCannotReadPrivateBuildInAnyState(String state) {
        practice.updateImageStatus(state, "registry/repo:prob-9");
        assertThatThrownBy(() -> detail(2)).isInstanceOf(ProblemNotFoundException.class);
        verifyNoInteractions(practices);
    }

    @ParameterizedTest @ValueSource(strings = {"PENDING", "FAILED", "FUTURE_STATE"})
    void unfinishedPublicProblemIsStillHiddenFromOtherUsers(String state) {
        problem.updateProblem(null, null, null, "public");
        practice.updateImageStatus(state, null);
        assertThatThrownBy(() -> detail(2)).isInstanceOf(ProblemNotFoundException.class);
    }

    @ParameterizedTest @ValueSource(strings = {"registry/repo:prob-9", "registry/repo@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void legacyTagsAndNewDigestsBothRemainReady(String image) {
        problem.updateProblem(null, null, null, "public");
        practice.updateImageStatus("READY", image);
        var status = detail(2).getPracticeBuild();
        assertThat(status.status()).isEqualTo("READY");
        assertThat(status.canStart()).isTrue();
    }

    @Test void readingOwnGroupProblemDoesNotGrantExecutionMembership() {
        problem.updateProblem(null, null, null, "group");
        practice.updateImageStatus("READY", "registry/repo:prob-9");
        when(groupProblems.findAllByProblemId(9L)).thenReturn(List.of());
        var status = detail(1).getPracticeBuild();
        assertThat(status.status()).isEqualTo("READY");
        assertThat(status.canStart()).isFalse();
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"FAILED", "FAULT", "STOPPED", "internal-host:private-log-value"})
    void failureDetailUsesFixedSafeMessage(String reason) throws Exception {
        practice.updateImageStatus("FAILED", "private-registry/repository:private-tag");
        ReflectionTestUtils.setField(practice, "buildFailureReason", reason);
        var response = detail(1);
        assertThat(response.getPracticeBuild().status()).isEqualTo("FAILED");
        assertThat(response.getPracticeBuild().canStart()).isFalse();
        assertThat(response.getPracticeBuild().message()).isEqualTo("실습 환경을 제작하지 못했습니다. 문제 번호와 함께 운영자에게 문의해 주세요.");
        var mapper = new ObjectMapper();
        var json = mapper.valueToTree(response);
        assertThat(json.path("practiceBuild").size()).isEqualTo(3);
        assertThat(json.path("practiceBuild").has("status")).isTrue();
        assertThat(json.path("practiceBuild").has("canStart")).isTrue();
        assertThat(json.path("practiceBuild").has("message")).isTrue();
        assertThat(mapper.writeValueAsString(json)).doesNotContain("private-registry", "private-source", "private-flag-hash", "private-project", "internal-request-id", "internal-host", "buildFailureReason", "codeBuildId", "ecrImageUri");
    }

    @ParameterizedTest @ValueSource(strings = {"TIMED_OUT", "DEADLINE_EXCEEDED"})
    void timeoutShowsActionableSafeMessage(String reason) {
        practice.updateImageStatus("FAILED", null);
        ReflectionTestUtils.setField(practice, "buildFailureReason", reason);
        assertThat(detail(1).getPracticeBuild().message()).contains("제한 시간을 초과");
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {"FUTURE_STATE"})
    void missingOrUnknownStateCannotEnablePractice(String state) {
        practice.updateImageStatus(state, null);
        var status = detail(1).getPracticeBuild();
        assertThat(status.status()).isEqualTo("UNAVAILABLE");
        assertThat(status.canStart()).isFalse();
    }
}
