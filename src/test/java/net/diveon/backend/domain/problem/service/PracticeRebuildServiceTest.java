package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.request.PracticeRebuildRequest;
import net.diveon.backend.domain.problem.entity.Problem;
import net.diveon.backend.domain.problem.entity.ProblemPractice;
import net.diveon.backend.domain.problem.repository.ProblemPracticeRepository;
import net.diveon.backend.domain.problem.repository.ProblemRepository;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.global.exception.ProblemAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeRebuildServiceTest {
    final ProblemRepository problems = mock(ProblemRepository.class);
    final ProblemPracticeRepository practices = mock(ProblemPracticeRepository.class);
    final S3Service s3 = mock(S3Service.class);
    final EcrService ecr = mock(EcrService.class);
    final PracticeBuildProperties props = new PracticeBuildProperties();
    final PracticeRebuildService service = new PracticeRebuildService(problems, practices, s3, ecr, props,
            Clock.fixed(START, ZoneOffset.UTC));
    final ProblemPractice practice = pending();

    @BeforeEach void readyRestrictedPractice() {
        props.setEnabled(true);
        practice.restrictCommands(List.of("ls"));
        practice.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START);
        var author = mock(User.class);
        when(author.getId()).thenReturn(1L);
        var problem = mock(Problem.class);
        when(problem.getAuthor()).thenReturn(author);
        when(problems.findById(7L)).thenReturn(Optional.of(problem));
        when(practices.findBuildForUpdate(7L)).thenReturn(Optional.of(practice));
        when(s3.hasDockerfileZip(7L, REQUEST)).thenReturn(true);
    }

    PracticeRebuildRequest request(List<String> commands, boolean newFile) {
        var request = new PracticeRebuildRequest();
        request.setCommandAllowlist(commands);
        if (newFile) request.setDockerfile(new MockMultipartFile("dockerfile", "source.zip", "application/zip", new byte[]{1}));
        return request;
    }

    @Test void allowlistOnlyChangeCopiesTheStoredSourceAndCleansUpTheOldImage() {
        var result = service.rebuild(1, 7, request(List.of("cat", "ls"), false));
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(practice.getBuildRequestId()).isNotEqualTo(REQUEST);
        assertThat(practice.getCommandAllowlist()).containsExactly("cat", "ls");
        assertThat(practice.getEcrImageUri()).isNull();
        verify(s3).copyDockerfileZip(7L, REQUEST, practice.getBuildRequestId(), List.of("cat", "ls"));
        verify(s3).deleteDockerfileZip(7L, REQUEST);
        verify(ecr).deleteImage(7L, BUILD, List.of("ls"));
    }

    @Test void newDockerfileIsUploadedAndAnEmptyListRemovesTheRestriction() {
        var request = request(List.of(), true);
        service.rebuild(1, 7, request);
        assertThat(practice.getCommandAllowlist()).isNull();
        verify(s3).uploadDockerfileZip(7L, practice.getBuildRequestId(), request.getDockerfile(), null);
        verify(s3, never()).copyDockerfileZip(anyLong(), any(), any(), any());
    }

    @Test void onlyTheAuthorMayRebuild() {
        assertThatThrownBy(() -> service.rebuild(2, 7, request(List.of("ls"), false))).isInstanceOf(ProblemAccessDeniedException.class);
        assertThat(practice.getBuildRequestId()).isEqualTo(REQUEST);
        verifyNoInteractions(s3, ecr);
    }

    @Test void runningBuildIsNotReplaced() {
        var building = pending();
        when(practices.findBuildForUpdate(7L)).thenReturn(Optional.of(building));
        assertThatThrownBy(() -> service.rebuild(1, 7, request(List.of("ls"), false)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verifyNoInteractions(s3, ecr);
    }

    @Test void missingOldSourceAsksForADockerfileBeforeChangingAnything() {
        when(s3.hasDockerfileZip(7L, REQUEST)).thenReturn(false);
        assertThatThrownBy(() -> service.rebuild(1, 7, request(List.of("ls"), false)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        assertThat(practice.getImageStatus()).isEqualTo("READY");
        verify(s3, never()).copyDockerfileZip(anyLong(), any(), any(), any());
        verifyNoInteractions(ecr);
    }

    @Test void invalidAllowlistIsRejectedBeforeAnyLookup() {
        assertThatThrownBy(() -> service.rebuild(1, 7, request(List.of("ls; id"), true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(problems, practices, s3, ecr);
    }

    @Test void failedCleanupNeverFailsTheRebuild() {
        doThrow(new IllegalStateException("ecr")).when(ecr).deleteImage(anyLong(), any(), any());
        doThrow(new IllegalStateException("s3")).when(s3).deleteDockerfileZip(anyLong(), any());
        assertThat(service.rebuild(1, 7, request(List.of("ls"), false)).status()).isEqualTo("PENDING");
    }
}
