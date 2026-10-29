package net.diveon.backend.domain.vm.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TerminalGrantStoreTest {
    @Test @SuppressWarnings("unchecked")
    void consumesWithSingleAtomicGetDeleteAndKeepsLeaseNamespaceSeparate() {
        RedisTemplate<String, String> redis = mock(RedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.getAndDelete("terminal:v1:ticket:t")).thenReturn("grant", (String) null);
        var store = new TerminalGrantStore(redis);
        assertEquals("grant", store.consume("t")); assertNull(store.consume("t"));
        store.lease("t");
        verify(ops, times(2)).getAndDelete("terminal:v1:ticket:t");
        verify(ops).get("terminal:v1:lease:t");
        verifyNoMoreInteractions(ops);
    }
}
