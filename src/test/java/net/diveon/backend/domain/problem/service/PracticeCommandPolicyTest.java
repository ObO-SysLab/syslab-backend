package net.diveon.backend.domain.problem.service;

import net.diveon.backend.domain.problem.dto.response.PracticeBuildStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import static net.diveon.backend.domain.problem.service.PracticeBuildStateTest.*;
import static org.assertj.core.api.Assertions.*;

class PracticeCommandPolicyTest {
    @Test void normalizesToSortedUniqueTrimmedNames() {
        assertThat(PracticeCommandPolicy.normalize(Arrays.asList(" ls", "cat", "ls", "", null, "python3.11", "g++", "x_y-z")))
                .containsExactly("cat", "g++", "ls", "python3.11", "x_y-z");
    }

    @Test void nothingEnteredMeansUnrestricted() {
        assertThat(PracticeCommandPolicy.normalize(null)).isNull();
        assertThat(PracticeCommandPolicy.normalize(List.of())).isNull();
        assertThat(PracticeCommandPolicy.normalize(List.of(" ", ""))).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-rf", ".hidden", "a b", "$(id)", "ls;id", "/bin/ls", "../x", "명령", "a,b"})
    void rejectsAnythingButCommandNames(String name) {
        assertThatThrownBy(() -> PracticeCommandPolicy.normalize(List.of(name))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void limitsLengthAndCount() {
        assertThat(PracticeCommandPolicy.normalize(List.of("a".repeat(32)))).containsExactly("a".repeat(32));
        assertThatThrownBy(() -> PracticeCommandPolicy.normalize(List.of("a".repeat(33)))).isInstanceOf(IllegalArgumentException.class);
        List<String> thirty = new ArrayList<>(IntStream.range(0, 30).mapToObj(i -> "c" + i).toList());
        assertThat(PracticeCommandPolicy.normalize(thirty)).hasSize(30);
        thirty.add("c30");
        assertThatThrownBy(() -> PracticeCommandPolicy.normalize(thirty)).isInstanceOf(IllegalArgumentException.class);
    }

    // Same vectors as syslab-infra vm/codebuild/test_build_image.py; both sides must stay identical.
    @Test void metadataAndMarkerMatchTheBuildScript() {
        assertThat(PracticeCommandPolicy.metadataValue(List.of("cat", "ls"))).isEqualTo("v1:cat,ls");
        assertThat(PracticeCommandPolicy.markerTag(BUILD, List.of("cat", "ls")))
                .isEqualTo("build-00000000-0000-4000-8000-000000000002-cmd-eeba41b2b2019ca3");
        assertThat(PracticeCommandPolicy.markerTag(BUILD, List.of("find", "strings", "xxd")))
                .endsWith("-cmd-7a12e1cb6c42c378");
    }

    @Test void unnormalizedPolicyIsNeverEncoded() {
        assertThatThrownBy(() -> PracticeCommandPolicy.metadataValue(List.of("ls", "cat"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PracticeCommandPolicy.markerTag(BUILD, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void policyIsFixedOnceTheImageIsBound() {
        var practice = pending();
        practice.restrictCommands(List.of("ls"));
        assertThat(practice.getCommandAllowlist()).containsExactly("ls");
        practice.applyBuild(REQUEST, BUILD, "SUCCEEDED", IMAGE, START);
        assertThatThrownBy(() -> practice.restrictCommands(null)).isInstanceOf(IllegalStateException.class);
        assertThat(practice.getCommandAllowlist()).containsExactly("ls");
    }

    @Test void unverifiedRestrictionIsATerminalFailureWithItsOwnReason() {
        var practice = pending();
        practice.restrictCommands(List.of("ls"));
        practice.applyBuild(REQUEST, BUILD, "IN_PROGRESS", null, START);
        practice.applyBuild(REQUEST, BUILD, "RESTRICTION_UNVERIFIED", null, START);
        assertThat(practice.getImageStatus()).isEqualTo("FAILED");
        assertThat(practice.getBuildFailureReason()).isEqualTo("RESTRICTION_UNVERIFIED");
        assertThat(PracticeBuildStatusResponse.of(practice, true).message()).contains("허용 명령어");
    }

    @Test void onlyTheAuthorSeesTheBuildFailureReason() {
        var practice = pending();
        practice.applyBuild(REQUEST, BUILD, "FAILED", null, START, "COMMAND_NOT_FOUND:xxd");
        assertThat(PracticeBuildStatusResponse.of(practice, true, true).failure())
                .isEqualTo(new PracticeBuildStatusResponse.Failure("COMMAND_NOT_FOUND", "xxd"));
        assertThat(PracticeBuildStatusResponse.of(practice, true, false).failure()).isNull();
        var other = pending();
        other.applyBuild(REQUEST, BUILD, "FAILED", null, START, "DOCKERFILE_FAILED");
        assertThat(PracticeBuildStatusResponse.of(other, true, true).failure())
                .isEqualTo(new PracticeBuildStatusResponse.Failure("DOCKERFILE_FAILED", null));
    }

    @Test void newRequestClearsTheOldReason() {
        var practice = pending();
        practice.applyBuild(REQUEST, BUILD, "FAILED", null, START, "DOCKERFILE_FAILED");
        practice.requestBuild(REQUEST.replace("001", "009"), START, START.plusSeconds(600));
        assertThat(practice.getBuildFailureDetail()).isNull();
    }

    @Test void unrestrictedFailureKeepsTheGeneralMessage() {
        var practice = pending();
        practice.applyBuild(REQUEST, BUILD, "FAILED", null, START);
        assertThat(PracticeBuildStatusResponse.of(practice, true).message()).doesNotContain("허용 명령어");
    }
}
