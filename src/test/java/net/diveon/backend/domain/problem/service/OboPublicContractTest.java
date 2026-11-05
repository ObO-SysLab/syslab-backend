package net.diveon.backend.domain.problem.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OboPublicContractTest {
    private final ObjectMapper mapper=new ObjectMapper();
    @Test void diagramExposesOnlySubmissionContract() throws Exception {
        var document=mapper.readTree("""
          {"mode":"coding_diff","codingDiff":{"nodes":[{"id":"queue1","type":"queue-strip","position":{"x":123,"y":456},"data":{"label":"SECRET_LABEL","items":["SECRET_VALUE"],"capacity":43}}],"edges":[{"id":"e1","source":"SECRET_SOURCE","target":"SECRET_TARGET","data":{"label":"SECRET_EDGE"}}],"referenceTrace":[{"secret":"SECRET_TRACE"}],"sourceTraceText":"SECRET_RAW"}}
          """);
        var before=document.deepCopy();var result=OboPublicContract.describe(document);
        assertEquals(before,document);
        assertEquals(4,result.size());
        assertEquals(3,result.path("nodes").get(0).size());
        assertEquals("queue1",result.at("/nodes/0/id").asText());
        assertEquals("e1",result.at("/edgeIds/0").asText());
        assertTrue(result.at("/nodes/0/fields/items").isTextual());
        assertFalse(result.toString().contains("SECRET"));
        assertFalse(result.toString().contains("position"));
        assertFalse(result.toString().contains("43"));
    }
    @Test void legacyAndTextOnlyDocumentsDoNotAcquireSemanticGrading() throws Exception {
        assertNull(OboPublicContract.describe(null));
        assertNull(OboPublicContract.describe(mapper.readTree("{\"mode\":\"single\",\"single\":{}}")));
    }
}
