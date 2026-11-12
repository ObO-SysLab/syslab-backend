package net.diveon.backend.domain.user.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.diveon.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProfileShowResponseTest {

    private ProfileShowResponse.UserInfo info(String interest) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 13L);
        ReflectionTestUtils.setField(user, "tier", 2);
        ReflectionTestUtils.setField(user, "score", 240);
        ReflectionTestUtils.setField(user, "interest", interest);
        return new ProfileShowResponse.UserInfo(user);
    }

    @Test
    void carriesIdTierAndScore() {
        var info = info(null);
        assertEquals(13L, info.getUserId());
        assertEquals(2, info.getTier());
        assertEquals(240, info.getScore());
        assertEquals(List.of(), info.getInterest());
    }

    @Test
    void blankInterestIsNotAnEmptyTag() {
        assertEquals(List.of(), info("").getInterest());
        assertEquals(List.of("Kernel", "Memory"), info(" Kernel, ,Memory ").getInterest());
    }
}
