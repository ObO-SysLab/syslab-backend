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
    @Test void plainOutputPublishesOnlyTheLineShape() throws Exception {
        var document=mapper.readTree("""
          {"mode":"coding_diff","codingDiff":{"nodes":[{"id":"memory","type":"slot-grid","position":{"x":1,"y":2},"data":{"label":"SECRET_LABEL","slotCount":3,"slots":[77,null,null]}},{"id":"queue1","type":"queue-strip","position":{"x":0,"y":0},"data":{"label":"SECRET_Q","items":["SECRET_VALUE"],"capacity":43}}],
           "edges":[{"id":"e1","source":"memory","target":"queue1"}],"referenceTrace":[],"frames":[{"id":"f1","label":"SECRET_CAPTION","highlightNodes":[],"highlightEdges":[]}],
           "outputFormat":{"kind":"lines-v1","target":"memory","blank":"-","markChanged":true}}}
          """);
        var result=OboPublicContract.describe(document);
        assertEquals("lines-v1",result.at("/outputFormat/kind").asText());
        assertEquals("slot-grid",result.at("/outputFormat/targetType").asText());
        assertEquals(3,result.at("/outputFormat/cells").asInt());
        assertEquals("-",result.at("/outputFormat/blank").asText());
        assertTrue(result.path("nodes").isMissingNode());
        assertFalse(result.toString().contains("SECRET"));
        assertFalse(result.toString().contains("memory"));
        assertFalse(result.toString().contains("77"));
        assertFalse(result.toString().contains("43"));
    }
    @Test void legacyAndTextOnlyDocumentsDoNotAcquireSemanticGrading() throws Exception {
        assertNull(OboPublicContract.describe(null));
        assertNull(OboPublicContract.describe(mapper.readTree("{\"mode\":\"single\",\"single\":{}}")));
    }
}
