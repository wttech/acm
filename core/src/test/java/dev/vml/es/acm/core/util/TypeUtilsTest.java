package dev.vml.es.acm.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TypeUtilsTest {

    @Test
    void shouldKeepValueAlreadyMatchingType() {
        assertEquals("value", TypeUtils.convert("value", String.class, true).orElse(null));
    }

    @Test
    void shouldConvertStringCollectionToArray() {
        String[] result = TypeUtils.convert(Arrays.asList("first", "second"), String[].class, true)
                .orElse(null);

        assertArrayEquals(new String[] {"first", "second"}, result);
    }
}
