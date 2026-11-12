package net.diveon.backend.domain.activity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class RecentActivityServiceTest {

    @Test
    void limitIsClamped() {
        assertEquals(10, RecentActivityService.clampLimit(null));
        assertEquals(10, RecentActivityService.clampLimit(0));
        assertEquals(5, RecentActivityService.clampLimit(5));
        assertEquals(20, RecentActivityService.clampLimit(500));
    }

    @Test
    void onlyPublicProblemsNewestFirstWithSubmittedTime() {
        String sql = RecentActivityService.RECENT_SQL;
        assertTrue(sql.contains("ps.visibility = 'public'"));
        assertTrue(sql.contains("ORDER BY sr.id DESC"));
        assertTrue(sql.contains("ss.submitted_at"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void passesClampedLimitToQuery() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());
        new RecentActivityService(jdbc).getRecent(99);
        verify(jdbc).query(eq(RecentActivityService.RECENT_SQL), any(RowMapper.class), eq(20));
    }
}
