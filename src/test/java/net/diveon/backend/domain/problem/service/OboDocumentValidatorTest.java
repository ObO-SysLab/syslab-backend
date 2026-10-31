package net.diveon.backend.domain.problem.service;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;
import org.junit.jupiter.api.Test;

class OboDocumentValidatorTest {
    final ObjectMapper mapper=new ObjectMapper();
    final OboDocumentValidator validator=new OboDocumentValidator(mapper,"node","obo-runtime/checker.mjs");
    @Test void legacyNullAndGraphRemainCompatible() throws Exception {
        validator.validate(null);
        validator.validate(mapper.readTree("{\"mode\":\"single\",\"single\":{\"nodes\":[],\"edges\":[],\"frames\":[]}}"));
    }
    @Test void actualCheckerRejectsMalformedModelDocument() throws Exception {
        var document=mapper.readTree("{\"schemaVersion\":2,\"mode\":\"simulation\",\"simulation\":{\"schemaVersion\":2,\"instances\":[]}}");
        assertThrows(InvalidSubmissionRequestException.class,()->validator.validate(document));
    }
    @Test void checkerUnavailabilityIsNotMisreportedAsInvalidAuthorInput() throws Exception {
        var unavailable=new OboDocumentValidator(mapper,"missing-node-executable","obo-runtime/checker.mjs");
        var document=mapper.readTree("{\"schemaVersion\":2,\"mode\":\"simulation\",\"simulation\":{}}");
        assertThrows(IllegalStateException.class,()->unavailable.validate(document));
    }
}
