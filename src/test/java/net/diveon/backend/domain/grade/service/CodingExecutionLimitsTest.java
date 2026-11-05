package net.diveon.backend.domain.grade.service;

import java.util.Collections;
import java.util.List;
import net.diveon.backend.domain.problem.others.ForDtoTestCase;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CodingExecutionLimitsTest {
    final ForDtoTestCase empty = new ForDtoTestCase(1, "", "", true);
    void validate(Integer time, Integer memory, List<ForDtoTestCase> cases) {
        CodingExecutionLimits.problem(time, memory, List.of("python", "c", "cpp"), cases);
    }
    @ParameterizedTest @ValueSource(ints = {0, -1, 999, 1001, 1500, 30001, Integer.MAX_VALUE})
    void rejectsUnrepresentableTimeWithoutRounding(int time) {
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(time, 128, List.of(empty)));
    }
    @ParameterizedTest @ValueSource(ints = {0, 15, 1025, Integer.MAX_VALUE})
    void rejectsUnsupportedMemory(int memory) {
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, memory, List.of(empty)));
    }
    @Test void permitsExactBoundaryAndEmptyStdin() {
        assertEquals(30, CodingExecutionLimits.seconds(30000));
        assertDoesNotThrow(() -> validate(30000, 1024, Collections.nCopies(18, empty)));
        assertDoesNotThrow(() -> validate(1000, 16, Collections.nCopies(100, empty)));
    }
    @Test void rejectsTotalBudgetBeforeRunnerWouldRejectIt() {
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(30000, 128, Collections.nCopies(19, empty)));
    }
    @Test void boundsCount() {
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, List.of()));
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, Collections.nCopies(101, empty)));
    }
    @Test void checksUtf8BytesAndBundleExpansion() {
        var large = new ForDtoTestCase(1, "가".repeat(350000), "", false);
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, List.of(large)));
        var mib = new ForDtoTestCase(1, "x".repeat(1024 * 1024), "", false);
        assertDoesNotThrow(() -> validate(1000, 128, List.of(mib)));
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, Collections.nCopies(12, mib)));
    }
    @Test void rejectsMissingConstraintsAndNullCaseData() {
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(null, 128, List.of(empty)));
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, null, List.of(empty)));
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, null));
        assertThrows(InvalidSubmissionRequestException.class, () -> validate(1000, 128, List.of(new ForDtoTestCase())));
    }
    @Test void rejectsUnsupportedLanguages() {
        assertThrows(InvalidSubmissionRequestException.class, () -> CodingExecutionLimits.problem(1000, 128, List.of("java"), List.of(empty)));
    }
}
