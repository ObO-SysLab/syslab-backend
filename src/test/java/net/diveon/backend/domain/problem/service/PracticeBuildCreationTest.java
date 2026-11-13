package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.entity.*;
import net.diveon.backend.domain.problem.repository.*;
import net.diveon.backend.domain.problem.dto.request.ProblemCreatePracticeRequest;
import net.diveon.backend.domain.user.entity.User;
import net.diveon.backend.domain.user.repository.UserRepository;
import net.diveon.backend.domain.group.repository.*;
import net.diveon.backend.domain.contest.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.Optional;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.START;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PracticeBuildCreationTest {
    final ProblemPracticeRepository practices = mock(ProblemPracticeRepository.class);
    final ProblemRepository problems = mock(ProblemRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final S3Service s3 = mock(S3Service.class);
    final PracticeBuildProperties props = new PracticeBuildProperties();
    final ProblemCreateService service = new ProblemCreateService(mock(ProblemObjectiveRepository.class), practices,
            mock(ProblemCodingRepository.class), problems, users, mock(OboStepRepository.class), s3,
            mock(GroupRepository.class), mock(GroupProblemRepository.class), mock(ContestRepository.class),
            mock(ContestProblemRepository.class), mock(ContestParticipantRepository.class), props, Clock.fixed(START, ZoneOffset.UTC),
            mock(OboDocumentValidator.class), mock(ProblemRevisionService.class));

    @Test void disabledCreationHasNoDatabaseOrS3SideEffect() {
        assertThatThrownBy(() -> service.createPractice(1, new ProblemCreatePracticeRequest())).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(problems, practices, users, s3);
    }

    ProblemCreatePracticeRequest enabledRequest() {
        props.setEnabled(true);
        when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(problems.save(any())).thenAnswer(invocation -> {
            Problem p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 7L);
            ReflectionTestUtils.setField(p, "createdAt", LocalDateTime.of(2026,10,30,0,0));
            return p;
        });
        var request = new ProblemCreatePracticeRequest();
        request.setFlag("flag"); request.setVisibility("public"); request.setTitle("practice");
        request.setDockerfile(new MockMultipartFile("dockerfile", "source.zip", "application/zip", new byte[]{1}));
        return request;
    }

    @Test void legacyHiddenAllowedCommandsNeverRestrictTheImage() {
        var request = enabledRequest();
        // Old pages post the removed allowedCommands field; it is not bound and never restricts anything.
        service.createPractice(1, request);
        var saved = ArgumentCaptor.forClass(ProblemPractice.class);
        verify(practices).save(saved.capture());
        assertThat(saved.getValue().getCommandAllowlist()).isNull();
        verify(s3).uploadDockerfileZip(eq(7L), anyString(), any(), isNull());
    }

    @Test void allowlistIsNormalizedStoredAndSentWithTheSource() {
        var request = enabledRequest();
        request.setCommandAllowlist(java.util.List.of("ls", " cat", "ls"));
        service.createPractice(1, request);
        var saved = ArgumentCaptor.forClass(ProblemPractice.class);
        verify(practices).save(saved.capture());
        assertThat(saved.getValue().getCommandAllowlist()).containsExactly("cat", "ls");
        verify(s3).uploadDockerfileZip(7L, saved.getValue().getBuildRequestId(), request.getDockerfile(), java.util.List.of("cat", "ls"));
    }

    @Test void hintIsStoredTrimmedAndBlankHintIsNone() {
        var request = enabledRequest();
        request.setHint("  find 의 -inum 을 써 보세요.\n");
        service.createPractice(1, request);
        var saved = ArgumentCaptor.forClass(ProblemPractice.class);
        verify(practices).save(saved.capture());
        assertThat(saved.getValue().getHint()).isEqualTo("find 의 -inum 을 써 보세요.");

        var blank = new ProblemPractice();
        blank.describeHint("   ");
        assertThat(blank.getHint()).isNull();
    }

    @Test void invalidAllowlistIsRejectedBeforeAnyWrite() {
        var request = enabledRequest();
        request.setCommandAllowlist(java.util.List.of("ls; rm -rf /"));
        assertThatThrownBy(() -> service.createPractice(1, request))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        verifyNoInteractions(problems, practices, s3);
    }

    @Test void requestAndTenMinuteDeadlineAreStoredBeforeExactSourceUpload() {
        var request = enabledRequest();
        service.createPractice(1, request);
        var saved = ArgumentCaptor.forClass(ProblemPractice.class);
        var order = inOrder(practices, s3);
        order.verify(practices).save(saved.capture());
        var practice = saved.getValue();
        order.verify(s3).uploadDockerfileZip(7L, practice.getBuildRequestId(), request.getDockerfile(), null);
        assertThat(practice.getBuildRequestedAt()).isEqualTo(START);
        assertThat(practice.getBuildDeadlineAt()).isEqualTo(START.plusSeconds(600));
        assertThat(PracticeBuildIdentity.fromKey(new PracticeBuildIdentity(7, practice.getBuildRequestId()).sourceKey()).problemId()).isEqualTo(7);
    }
}
