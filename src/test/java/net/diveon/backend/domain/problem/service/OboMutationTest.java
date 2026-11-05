package net.diveon.backend.domain.problem.service;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import net.diveon.backend.global.exception.InvalidSubmissionRequestException;
import org.junit.jupiter.api.Test;
class OboMutationTest {
    @Test void missingActionAndLegacyNullPreserveExistingDocument(){
        var current=JsonNodeFactory.instance.objectNode().put("mode","single");
        assertSame(current,ProblemUpdateService.mutateObo(null,null,current));
        assertSame(current,ProblemUpdateService.mutateObo("keep",null,current));
        assertNull(ProblemUpdateService.mutateObo("remove",null,current));
    }
    @Test void replaceRequiresDataAndCopiesIt(){
        var supplied=JsonNodeFactory.instance.objectNode().put("mode","single");
        assertThrows(InvalidSubmissionRequestException.class,()->ProblemUpdateService.mutateObo("replace",null,supplied));
        var result=ProblemUpdateService.mutateObo("replace",supplied,null);
        assertEquals(supplied,result);assertNotSame(supplied,result);
        assertThrows(InvalidSubmissionRequestException.class,()->ProblemUpdateService.mutateObo("unknown",supplied,null));
    }
}
